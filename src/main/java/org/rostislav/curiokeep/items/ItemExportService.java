package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionModuleRepository;
import org.rostislav.curiokeep.collections.CollectionRepository;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes a collection's items out as a file the owner can keep: JSON that {@link ItemImportService} reads back, or CSV for a
 * spreadsheet. Items are read in batches ordered by creation time and id (no OFFSET), so the memory used does not grow with the
 * size of the collection.
 * <p>
 * Cover images are not part of the file. Attributes that point at a locally stored cover ({@code /api/assets/...}) are left
 * out because the path means nothing elsewhere; the image files themselves live in the assets directory.
 */
@Service
public class ItemExportService {

    static final String FORMAT = "curiokeep-export";
    static final int VERSION = 1;
    static final String LOCAL_ASSET_PREFIX = "/api/assets/";
    private static final int BATCH_SIZE = 500;
    private static final java.util.Set<String> CSV_BUILT_IN_COLUMNS = java.util.Set.of("state", "title", "created_at");
    private static final String CSV_BOM = "﻿";
    private static final String CSV_EOL = "\r\n";

    public record ExportFile(String fileName, MediaType mediaType, StreamingResponseBody body) {
    }

    private record ExportModule(ModuleDefinitionEntity entity, ModuleContract contract) {
    }

    record ExportedIdentifier(String type, String value) {
    }

    record ExportedItem(String module, String state, String title, Map<String, Object> attributes,
                        List<ExportedIdentifier> identifiers, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    @FunctionalInterface
    private interface BatchWriter {
        void write(List<ItemEntity> batch, Map<UUID, List<ItemIdentifierEntity>> identifiers) throws IOException;
    }

    private final ItemRepository items;
    private final ItemIdentifierRepository identifiers;
    private final CollectionRepository collections;
    private final CollectionModuleRepository collectionModules;
    private final ModuleDefinitionRepository moduleDefinitions;
    private final ModuleQueryService modules;
    private final CollectionAccessService access;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ItemExportService(ItemRepository items, ItemIdentifierRepository identifiers, CollectionRepository collections,
                             CollectionModuleRepository collectionModules, ModuleDefinitionRepository moduleDefinitions,
                             ModuleQueryService modules, CollectionAccessService access, CurrentUserService currentUser,
                             ObjectMapper objectMapper, Clock clock) {
        this.items = items;
        this.identifiers = identifiers;
        this.collections = collections;
        this.collectionModules = collectionModules;
        this.moduleDefinitions = moduleDefinitions;
        this.modules = modules;
        this.access = access;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Checks access and what can be exported, then returns the file description and a body that streams the items. Everything
     * that can fail for the caller fails here, before the response has started.
     *
     * @param moduleId the module to export, or null for every module enabled in the collection (JSON only)
     */
    @Transactional(readOnly = true)
    public ExportFile open(UUID collectionId, ExportFormat format, UUID moduleId) {
        access.requireRole(collectionId, currentUser.requireCurrentUser().getId(), Role.VIEWER);
        CollectionEntity collection = collections.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND"));

        List<ExportModule> enabled = enabledModules(collectionId);
        List<ExportModule> selected = moduleId == null ? enabled
                : enabled.stream().filter(m -> m.entity().getId().equals(moduleId)).toList();
        if (moduleId != null && selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_ENABLED");
        }
        if (format == ExportFormat.CSV && selected.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_REQUIRED");
        }

        String name = collection.getName();
        String description = collection.getDescription();
        String fileName = "curiokeep-" + slug(name) + "-" + LocalDate.now(clock) + "." + format.extension();
        StreamingResponseBody body = format == ExportFormat.JSON
                ? out -> writeJson(collectionId, name, description, selected, out)
                : out -> writeCsv(collectionId, selected.getFirst(), out);
        return new ExportFile(fileName, format.mediaType(), body);
    }

    private List<ExportModule> enabledModules(UUID collectionId) {
        List<UUID> ids = collectionModules.findAllByIdCollectionId(collectionId).stream()
                .map(CollectionModuleEntity::getModuleId).filter(Objects::nonNull).toList();
        return moduleDefinitions.findAllById(ids).stream()
                .sorted(Comparator.comparing(ModuleDefinitionEntity::getModuleKey))
                .map(def -> new ExportModule(def, modules.getContract(def)))
                .toList();
    }

    private void writeJson(UUID collectionId, String name, String description, List<ExportModule> selected, OutputStream out) throws IOException {
        try (JsonGenerator json = objectMapper.createGenerator(out)) {
            json.writeStartObject();
            json.writeStringProperty("format", FORMAT);
            json.writeNumberProperty("version", VERSION);
            json.writeStringProperty("exportedAt", clock.instant().toString());
            json.writeObjectPropertyStart("collection");
            json.writeStringProperty("name", name);
            if (description != null) json.writeStringProperty("description", description);
            json.writeEndObject();
            json.writeArrayPropertyStart("items");
            for (ExportModule module : selected) {
                String key = module.entity().getModuleKey();
                forEachBatch(collectionId, module.entity().getId(), (batch, ids) -> {
                    for (ItemEntity item : batch) {
                        json.writePOJO(toExported(key, item, ids.getOrDefault(item.getId(), List.of())));
                    }
                });
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private ExportedItem toExported(String moduleKey, ItemEntity item, List<ItemIdentifierEntity> ids) {
        Map<String, Object> attributes = attributesOf(item);
        attributes.entrySet().removeIf(e -> e.getValue() instanceof String s && s.startsWith(LOCAL_ASSET_PREFIX));
        List<ExportedIdentifier> exportedIds = ids.stream()
                .sorted(Comparator.comparing(i -> i.getIdType().name()))
                .map(i -> new ExportedIdentifier(i.getIdType().name(), i.getIdValue()))
                .toList();
        return new ExportedItem(moduleKey, item.getStateKey(), item.getTitle(), attributes, exportedIds, item.getCreatedAt(), item.getUpdatedAt());
    }

    private void writeCsv(UUID collectionId, ExportModule module, OutputStream out) throws IOException {
        // The item's own state, title and creation time have their own columns; a module field with the same key would repeat them.
        List<FieldContract> fields = module.contract().fields().stream().filter(f -> f.active() && !CSV_BUILT_IN_COLUMNS.contains(f.key()))
                .sorted(Comparator.comparingInt(FieldContract::order)).toList();
        ItemIdentifierEntity.IdType[] idTypes = ItemIdentifierEntity.IdType.values();
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(CSV_BOM);

        List<String> header = new ArrayList<>(List.of("state", "title"));
        fields.forEach(f -> header.add(f.key()));
        for (ItemIdentifierEntity.IdType type : idTypes) header.add("identifier_" + type.name().toLowerCase(Locale.ROOT));
        header.add("created_at");
        writeCsvRow(writer, header.stream().map(h -> (Object) h).toList());

        forEachBatch(collectionId, module.entity().getId(), (batch, ids) -> {
            for (ItemEntity item : batch) {
                Map<String, Object> attributes = attributesOf(item);
                List<Object> row = new ArrayList<>();
                row.add(item.getStateKey());
                row.add(item.getTitle());
                fields.forEach(f -> row.add(attributes.get(f.key())));
                Map<String, String> byType = ids.getOrDefault(item.getId(), List.of()).stream()
                        .collect(Collectors.toMap(i -> i.getIdType().name(), ItemIdentifierEntity::getIdValue, (a, b) -> a));
                for (ItemIdentifierEntity.IdType type : idTypes) row.add(byType.get(type.name()));
                row.add(item.getCreatedAt() == null ? null : item.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toString());
                writeCsvRow(writer, row);
            }
        });
        writer.flush();
    }

    private void writeCsvRow(Writer writer, List<Object> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) writer.write(',');
            writer.write(csvCell(cells.get(i)));
        }
        writer.write(CSV_EOL);
    }

    /**
     * One RFC 4180 cell. Text that a spreadsheet would run as a formula (it starts with = + - @, tab or carriage return) gets
     * a leading apostrophe so it stays text; numbers and booleans come from the data model, not from a user's text, and are
     * written as they are so negative numbers survive.
     */
    String csvCell(Object value) {
        if (value == null) return "";
        String text;
        boolean literal = value instanceof Number || value instanceof Boolean;
        if (value instanceof List<?> list) {
            text = list.stream().map(String::valueOf).collect(Collectors.joining("; "));
        } else if (value instanceof Map<?, ?>) {
            text = objectMapper.writeValueAsString(value);
        } else {
            text = String.valueOf(value);
        }
        if (!literal && !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean needsQuotes = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }

    private void forEachBatch(UUID collectionId, UUID moduleId, BatchWriter writer) throws IOException {
        List<ItemEntity> batch = items.findFirstBatch(collectionId, moduleId, PageRequest.of(0, BATCH_SIZE));
        while (!batch.isEmpty()) {
            Map<UUID, List<ItemIdentifierEntity>> ids = identifiers.findAllByItemIdIn(batch.stream().map(ItemEntity::getId).toList())
                    .stream().collect(Collectors.groupingBy(ItemIdentifierEntity::getItemId));
            writer.write(batch, ids);
            if (batch.size() < BATCH_SIZE) return;
            ItemEntity last = batch.getLast();
            batch = items.findBatchAfter(collectionId, moduleId, last.getCreatedAt(), last.getId(), PageRequest.of(0, BATCH_SIZE));
        }
    }

    private Map<String, Object> attributesOf(ItemEntity item) {
        String json = item.getAttributes();
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    static String slug(String name) {
        String slug = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.length() > 40) slug = slug.substring(0, 40).replaceAll("-+$", "");
        return slug.isEmpty() ? "collection" : slug;
    }
}
