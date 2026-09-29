package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionModuleRepository;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.items.api.dto.ImportResult;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adds the items of an export file to a collection. Every item is checked the way a normal save is checked (module enabled in
 * the collection, declared state, attributes against the module contract), an invalid item is reported and skipped, and the
 * rest are stored in batches. Covers are never downloaded here.
 * <p>
 * An import only ever adds: it does not look for duplicates, update or delete anything, so importing the same file twice
 * gives every item twice.
 */
@Service
public class ItemImportService {

    static final int MAX_ITEMS = 10_000;
    static final int MAX_REPORTED_ERRORS = 50;
    private static final int BATCH_SIZE = 200;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_IDENTIFIER_LENGTH = 200;
    private static final Logger log = LoggerFactory.getLogger(ItemImportService.class);

    private record EnabledModule(UUID id, ModuleContract contract) {
    }

    private record Prepared(int index, ItemEntity item, List<ItemIdentifierEntity> identifiers) {
    }

    private final ItemRepository items;
    private final ItemIdentifierRepository identifiers;
    private final CollectionModuleRepository collectionModules;
    private final ModuleDefinitionRepository moduleDefinitions;
    private final ModuleQueryService modules;
    private final CollectionAccessService access;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ItemImportService(ItemRepository items, ItemIdentifierRepository identifiers, CollectionModuleRepository collectionModules,
                             ModuleDefinitionRepository moduleDefinitions, ModuleQueryService modules, CollectionAccessService access,
                             CurrentUserService currentUser, ObjectMapper objectMapper, TransactionTemplate tx, Clock clock) {
        this.items = items;
        this.identifiers = identifiers;
        this.collectionModules = collectionModules;
        this.moduleDefinitions = moduleDefinitions;
        this.modules = modules;
        this.access = access;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.tx = tx;
        this.clock = clock;
    }

    public ImportResult importJson(UUID collectionId, byte[] content) {
        UUID userId = currentUser.requireCurrentUser().getId();
        access.requireRole(collectionId, userId, Role.EDITOR);

        JsonNode entries = itemsOf(parse(content));
        if (entries.size() > MAX_ITEMS) throw badRequest("TOO_MANY_ITEMS");
        Map<String, EnabledModule> enabled = enabledModules(collectionId);

        List<ImportResult.ItemError> errors = new ArrayList<>();
        int[] failed = {0};
        int imported = 0;
        List<Prepared> pending = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                pending.add(prepare(index, entries.get(index), collectionId, userId, enabled));
            } catch (ResponseStatusException invalid) {
                fail(index, invalid.getReason(), errors, failed);
            }
            if (pending.size() == BATCH_SIZE) imported += store(pending, errors, failed);
        }
        imported += store(pending, errors, failed);
        log.info("Items imported: collectionId={} imported={} failed={} byUserId={}", collectionId, imported, failed[0], userId);
        return new ImportResult(imported, failed[0], List.copyOf(errors));
    }

    private JsonNode parse(byte[] content) {
        try {
            return objectMapper.readTree(content);
        } catch (JacksonException e) {
            throw badRequest("INVALID_EXPORT_FILE");
        }
    }

    private JsonNode itemsOf(JsonNode root) {
        if (!root.isObject() || !ItemExportService.FORMAT.equals(text(root, "format"))) throw badRequest("INVALID_EXPORT_FILE");
        if (root.path("version").asInt(-1) != ItemExportService.VERSION) throw badRequest("UNSUPPORTED_EXPORT_VERSION");
        JsonNode entries = root.path("items");
        if (!entries.isArray()) throw badRequest("INVALID_EXPORT_FILE");
        return entries;
    }

    private Map<String, EnabledModule> enabledModules(UUID collectionId) {
        List<UUID> ids = collectionModules.findAllByIdCollectionId(collectionId).stream()
                .map(CollectionModuleEntity::getModuleId).filter(Objects::nonNull).toList();
        Map<String, EnabledModule> byKey = new HashMap<>();
        for (ModuleDefinitionEntity def : moduleDefinitions.findAllById(ids)) {
            byKey.put(def.getModuleKey(), new EnabledModule(def.getId(), modules.getContract(def)));
        }
        return byKey;
    }

    private Prepared prepare(int index, JsonNode entry, UUID collectionId, UUID userId, Map<String, EnabledModule> enabled) {
        if (!entry.isObject()) throw badRequest("INVALID_ITEM");
        String moduleKey = text(entry, "module");
        if (moduleKey == null) throw badRequest("MODULE_REQUIRED");
        EnabledModule module = enabled.get(moduleKey);
        if (module == null) throw badRequest("MODULE_NOT_ENABLED");

        String state = text(entry, "state");
        ItemStateRules.validate(module.contract(), state);

        ObjectNode attributes = attributesOf(entry);
        ItemAttributeValidator.validate(module.contract(), attributes);

        String title = text(entry, "title");
        if (title == null && attributes.path("title").isString()) title = attributes.path("title").asString();
        if (title != null && title.length() > MAX_TITLE_LENGTH) throw badRequest("INVALID_TITLE");

        OffsetDateTime created = timestamp(entry.path("createdAt"));
        ItemEntity item = new ItemEntity();
        item.setCollectionId(collectionId);
        item.setModuleId(module.id());
        item.setStateKey(ItemStateRules.normalize(state, module.contract()));
        item.setTitle(title);
        item.setAttributes(attributes.toString());
        item.setCreatedBy(userId);
        item.setCreatedAt(created);
        item.setUpdatedAt(created);
        return new Prepared(index, item, identifiersOf(entry.path("identifiers")));
    }

    private ObjectNode attributesOf(JsonNode entry) {
        JsonNode node = entry.path("attributes");
        if (node.isMissingNode() || node.isNull()) return objectMapper.createObjectNode();
        if (!node.isObject()) throw badRequest("INVALID_ATTRIBUTES");
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        // A path to a cover file on the server the export came from means nothing here.
        JsonNode image = copy.path("providerImageUrl");
        if (image.isString() && image.asString().startsWith(ItemExportService.LOCAL_ASSET_PREFIX)) copy.remove("providerImageUrl");
        return copy;
    }

    private List<ItemIdentifierEntity> identifiersOf(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return List.of();
        if (!node.isArray()) throw badRequest("INVALID_IDENTIFIER");
        Map<ItemIdentifierEntity.IdType, ItemIdentifierEntity> onePerType = new LinkedHashMap<>();
        for (JsonNode entry : node) {
            String type = text(entry, "type");
            String value = text(entry, "value");
            if (type == null || value == null || value.isBlank() || value.length() > MAX_IDENTIFIER_LENGTH) throw badRequest("INVALID_IDENTIFIER");
            ItemIdentifierEntity.IdType idType;
            try {
                idType = ItemIdentifierEntity.IdType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw badRequest("INVALID_IDENTIFIER");
            }
            ItemIdentifierEntity identifier = new ItemIdentifierEntity();
            identifier.setIdType(idType);
            identifier.setIdValue(value.trim());
            onePerType.put(idType, identifier);
        }
        return List.copyOf(onePerType.values());
    }

    private OffsetDateTime timestamp(JsonNode node) {
        if (node.isString()) {
            try {
                OffsetDateTime parsed = OffsetDateTime.parse(node.asString());
                if (!parsed.isAfter(OffsetDateTime.now(clock).plusDays(1))) return parsed;
            } catch (DateTimeParseException ignored) {
                // an unreadable or future time is replaced by now, it is not worth failing the item for
            }
        }
        return OffsetDateTime.now(clock);
    }

    /** Saves the pending items in one transaction and empties the list; a failed batch is reported item by item. */
    private int store(List<Prepared> pending, List<ImportResult.ItemError> errors, int[] failed) {
        if (pending.isEmpty()) return 0;
        List<Prepared> batch = List.copyOf(pending);
        pending.clear();
        try {
            tx.executeWithoutResult(status -> {
                items.saveAll(batch.stream().map(Prepared::item).toList());
                List<ItemIdentifierEntity> all = new ArrayList<>();
                for (Prepared prepared : batch) {
                    prepared.identifiers().forEach(id -> id.setItemId(prepared.item().getId()));
                    all.addAll(prepared.identifiers());
                }
                identifiers.saveAll(all);
            });
            return batch.size();
        } catch (RuntimeException e) {
            log.warn("Import batch of {} items failed: {}", batch.size(), e.getClass().getSimpleName());
            batch.forEach(prepared -> fail(prepared.index(), "SAVE_FAILED", errors, failed));
            return 0;
        }
    }

    private static void fail(int index, String reason, List<ImportResult.ItemError> errors, int[] failed) {
        failed[0]++;
        if (errors.size() < MAX_REPORTED_ERRORS) errors.add(new ImportResult.ItemError(index, reason));
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isString() ? value.asString() : null;
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
