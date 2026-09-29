package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionModuleRepository;
import org.rostislav.curiokeep.collections.CollectionRepository;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleId;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemExportServiceTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COMICS = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);

    @Mock
    ItemRepository items;
    @Mock
    ItemIdentifierRepository identifiers;
    @Mock
    CollectionRepository collections;
    @Mock
    CollectionModuleRepository collectionModules;
    @Mock
    ModuleDefinitionRepository moduleDefinitions;
    @Mock
    ModuleQueryService modules;
    @Mock
    CollectionAccessService access;
    @Mock
    CurrentUserService currentUser;

    final ObjectMapper mapper = new ObjectMapper();
    ItemExportService service;

    @BeforeEach
    void setUp() {
        service = new ItemExportService(items, identifiers, collections, collectionModules, moduleDefinitions, modules, access,
                currentUser, mapper, Clock.fixed(Instant.parse("2026-03-05T10:00:00Z"), ZoneOffset.UTC));
    }

    private static FieldContract field(String key, FieldType type, int order) {
        return new FieldContract(key, key, type, false, false, false, false, order, true, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of());
    }

    private ModuleDefinitionEntity module(UUID id, String key) {
        ModuleDefinitionEntity def = new ModuleDefinitionEntity();
        def.setId(id);
        def.setModuleKey(key);
        ModuleContract contract = new ModuleContract(key, "1.0.0", key, null, null, List.of(), List.of(),
                List.of(field("title", FieldType.TEXT, 1), field("pages", FieldType.NUMBER, 2), field("tags", FieldType.TAGS, 3)),
                List.of(), Map.of());
        when(modules.getContract(def)).thenReturn(contract);
        return def;
    }

    /** A collection named "My Books!" with the books module, and the comics module too when asked. */
    private void givenCollection(boolean withComics) {
        AppUserEntity user = new AppUserEntity();
        user.setId(USER);
        when(currentUser.requireCurrentUser()).thenReturn(user);
        CollectionEntity collection = new CollectionEntity();
        collection.setName("My Books!");
        collection.setDescription("Paper ones");
        when(collections.findById(COLLECTION)).thenReturn(java.util.Optional.of(collection));
        List<CollectionModuleEntity> enabled = new ArrayList<>();
        List<ModuleDefinitionEntity> defs = new ArrayList<>();
        enabled.add(enabled(BOOKS));
        defs.add(module(BOOKS, "books"));
        if (withComics) {
            enabled.add(enabled(COMICS));
            defs.add(module(COMICS, "comics"));
        }
        when(collectionModules.findAllByIdCollectionId(COLLECTION)).thenReturn(enabled);
        when(moduleDefinitions.findAllById(any())).thenReturn(defs);
    }

    private static CollectionModuleEntity enabled(UUID moduleId) {
        CollectionModuleEntity entity = new CollectionModuleEntity();
        entity.setId(new CollectionModuleId(COLLECTION, moduleId));
        return entity;
    }

    private static ItemEntity item(UUID moduleId, String title, String attributes, OffsetDateTime createdAt) {
        ItemEntity item = new ItemEntity();
        item.setId(UUID.randomUUID());
        item.setCollectionId(COLLECTION);
        item.setModuleId(moduleId);
        item.setStateKey("OWNED");
        item.setTitle(title);
        item.setAttributes(attributes);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(createdAt);
        return item;
    }

    private static ItemIdentifierEntity identifier(UUID itemId, ItemIdentifierEntity.IdType type, String value) {
        ItemIdentifierEntity id = new ItemIdentifierEntity();
        id.setItemId(itemId);
        id.setIdType(type);
        id.setIdValue(value);
        return id;
    }

    private String write(ItemExportService.ExportFile file) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        file.body().writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void jsonExportWritesTheFormatHeaderAndEveryModulesItems() throws Exception {
        givenCollection(true);
        ItemEntity dune = item(BOOKS, "Dune", "{\"title\":\"Dune\",\"pages\":412,\"providerImageUrl\":\"/api/assets/0123456789abcdef0123456789abcdef.png\"}", T0);
        ItemEntity hulk = item(COMICS, "Hulk", "{\"title\":\"Hulk\",\"providerImageUrl\":\"https://example.com/hulk.jpg\"}", T0.plusDays(1));
        when(items.findFirstBatch(eq(COLLECTION), eq(BOOKS), any())).thenReturn(List.of(dune));
        when(items.findFirstBatch(eq(COLLECTION), eq(COMICS), any())).thenReturn(List.of(hulk));
        when(identifiers.findAllByItemIdIn(any())).thenAnswer(inv -> {
            List<UUID> ids = new ArrayList<>(inv.getArgument(0));
            return ids.contains(dune.getId())
                    ? List.of(identifier(dune.getId(), ItemIdentifierEntity.IdType.ISBN13, "9780441172719"), identifier(dune.getId(), ItemIdentifierEntity.IdType.ASIN, "B000"))
                    : List.of();
        });

        ItemExportService.ExportFile file = service.open(COLLECTION, ExportFormat.JSON, null);
        JsonNode root = mapper.readTree(write(file));

        assertThat(root.path("format").asString()).isEqualTo("curiokeep-export");
        assertThat(root.path("version").asInt()).isEqualTo(1);
        assertThat(root.path("exportedAt").asString()).isEqualTo("2026-03-05T10:00:00Z");
        assertThat(root.path("collection").path("name").asString()).isEqualTo("My Books!");
        assertThat(root.path("collection").path("description").asString()).isEqualTo("Paper ones");
        JsonNode exported = root.path("items");
        assertThat(exported).hasSize(2);
        assertThat(exported.get(0).path("module").asString()).isEqualTo("books");
        assertThat(exported.get(0).path("state").asString()).isEqualTo("OWNED");
        assertThat(exported.get(0).path("title").asString()).isEqualTo("Dune");
        assertThat(exported.get(0).path("attributes").path("pages").asInt()).isEqualTo(412);
        assertThat(exported.get(0).path("identifiers")).hasSize(2);
        assertThat(exported.get(0).path("identifiers").get(0).path("type").asString()).isEqualTo("ASIN"); // sorted by type
        assertThat(exported.get(1).path("module").asString()).isEqualTo("comics");
    }

    @Test
    void aCoverStoredOnThisServerIsLeftOutButAnExternalUrlIsKept() throws Exception {
        givenCollection(true);
        when(items.findFirstBatch(eq(COLLECTION), eq(BOOKS), any())).thenReturn(List.of(
                item(BOOKS, "Local", "{\"title\":\"Local\",\"providerImageUrl\":\"/api/assets/0123456789abcdef0123456789abcdef.png\"}", T0)));
        when(items.findFirstBatch(eq(COLLECTION), eq(COMICS), any())).thenReturn(List.of(
                item(COMICS, "Remote", "{\"title\":\"Remote\",\"providerImageUrl\":\"https://example.com/c.jpg\"}", T0)));
        when(identifiers.findAllByItemIdIn(any())).thenReturn(List.of());

        JsonNode root = mapper.readTree(write(service.open(COLLECTION, ExportFormat.JSON, null)));

        assertThat(root.path("items").get(0).path("attributes").has("providerImageUrl")).isFalse();
        assertThat(root.path("items").get(1).path("attributes").path("providerImageUrl").asString()).isEqualTo("https://example.com/c.jpg");
    }

    @Test
    void readsBatchByBatchUntilAShortOneWithoutLoadingEverythingAtOnce() throws Exception {
        givenCollection(false);
        List<ItemEntity> full = new ArrayList<>();
        for (int i = 0; i < 500; i++) full.add(item(BOOKS, "Item " + i, "{}", T0.plusSeconds(i)));
        ItemEntity last = full.getLast();
        when(items.findFirstBatch(eq(COLLECTION), eq(BOOKS), any())).thenReturn(full);
        when(items.findBatchAfter(eq(COLLECTION), eq(BOOKS), eq(last.getCreatedAt()), eq(last.getId()), any()))
                .thenReturn(List.of(item(BOOKS, "Item 500", "{}", T0.plusSeconds(500))));
        when(identifiers.findAllByItemIdIn(any())).thenReturn(List.of());

        JsonNode root = mapper.readTree(write(service.open(COLLECTION, ExportFormat.JSON, BOOKS)));

        assertThat(root.path("items")).hasSize(501);
        verify(items, times(1)).findBatchAfter(any(), any(), any(), any(), any());
        verify(identifiers, times(2)).findAllByItemIdIn(any()); // one query per batch, not per item
    }

    @Test
    void anEmptyCollectionStillExportsAValidFile() throws Exception {
        givenCollection(false);
        when(items.findFirstBatch(eq(COLLECTION), eq(BOOKS), any())).thenReturn(List.of());

        JsonNode root = mapper.readTree(write(service.open(COLLECTION, ExportFormat.JSON, null)));

        assertThat(root.path("items").isArray()).isTrue();
        assertThat(root.path("items")).isEmpty();
    }

    @Test
    void csvHasABomAHeaderAndOneRowPerItem() throws Exception {
        givenCollection(false);
        ItemEntity dune = item(BOOKS, "Dune, Messiah", "{\"title\":\"Dune, Messiah\",\"pages\":-3,\"tags\":[\"sci-fi\",\"classic\"]}", T0);
        when(items.findFirstBatch(eq(COLLECTION), eq(BOOKS), any())).thenReturn(List.of(dune));
        when(identifiers.findAllByItemIdIn(any())).thenReturn(List.of(identifier(dune.getId(), ItemIdentifierEntity.IdType.ISBN13, "9780441172719")));

        String csv = write(service.open(COLLECTION, ExportFormat.CSV, BOOKS));
        String[] lines = csv.split("\r\n");

        assertThat(csv).startsWith("﻿");
        assertThat(lines[0]).isEqualTo("﻿state,title,pages,tags,identifier_isbn10,identifier_isbn13,identifier_upc,identifier_ean,identifier_asin,identifier_custom,created_at");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).isEqualTo("OWNED,\"Dune, Messiah\",-3,sci-fi; classic,,9780441172719,,,,,2026-01-02T03:04:05Z");
    }

    @Test
    void csvCellsAreQuotedAndCannotRunAsSpreadsheetFormulas() {
        assertThat(service.csvCell(null)).isEmpty();
        assertThat(service.csvCell("plain")).isEqualTo("plain");
        assertThat(service.csvCell("a,b")).isEqualTo("\"a,b\"");
        assertThat(service.csvCell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(service.csvCell("two\nlines")).isEqualTo("\"two\nlines\"");
        assertThat(service.csvCell("=HYPERLINK(\"http://evil\")")).startsWith("\"'=HYPERLINK");
        assertThat(service.csvCell("+1+1")).isEqualTo("'+1+1");
        assertThat(service.csvCell("-2+3")).isEqualTo("'-2+3");
        assertThat(service.csvCell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(service.csvCell("\tTAB")).isEqualTo("'\tTAB");
        // real numbers and booleans are data, not text a user typed, so a negative number stays a number
        assertThat(service.csvCell(-5)).isEqualTo("-5");
        assertThat(service.csvCell(2.5)).isEqualTo("2.5");
        assertThat(service.csvCell(true)).isEqualTo("true");
        assertThat(service.csvCell(List.of("a", "b,c"))).isEqualTo("\"a; b,c\"");
        assertThat(service.csvCell(List.of("=x", "y"))).isEqualTo("'=x; y");
    }

    @Test
    void csvNeedsExactlyOneModule() {
        givenCollection(true);

        assertThatThrownBy(() -> service.open(COLLECTION, ExportFormat.CSV, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("MODULE_REQUIRED");
                });
    }

    @Test
    void aCollectionWithOneModuleNeedsNoModuleIdForCsv() {
        givenCollection(false);

        assertThat(service.open(COLLECTION, ExportFormat.CSV, null).fileName()).endsWith(".csv");
    }

    @Test
    void aModuleThatIsNotEnabledInTheCollectionCannotBeExported() {
        givenCollection(false);

        assertThatThrownBy(() -> service.open(COLLECTION, ExportFormat.JSON, COMICS))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getReason()).isEqualTo("MODULE_NOT_ENABLED"));
    }

    @Test
    void anyViewerMayExportButNobodyElse() {
        AppUserEntity user = new AppUserEntity();
        user.setId(USER);
        when(currentUser.requireCurrentUser()).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(access).requireRole(COLLECTION, USER, Role.VIEWER);

        assertThatThrownBy(() -> service.open(COLLECTION, ExportFormat.JSON, null)).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(items, identifiers, collections);
        verify(access).requireRole(COLLECTION, USER, Role.VIEWER);
        verify(access, never()).requireRole(eq(COLLECTION), eq(USER), eq(Role.EDITOR));
    }

    @Test
    void theFileIsNamedAfterTheCollectionAndTheDay() {
        givenCollection(false);

        ItemExportService.ExportFile file = service.open(COLLECTION, ExportFormat.JSON, null);

        assertThat(file.fileName()).isEqualTo("curiokeep-my-books-2026-03-05.json");
        assertThat(file.mediaType().toString()).startsWith("application/json");
    }

    @Test
    void slugsAreSafeForAFileName() {
        assertThat(ItemExportService.slug("My Books!")).isEqualTo("my-books");
        assertThat(ItemExportService.slug("  --Ünï/cödé\\..  ")).isEqualTo("n-c-d");
        assertThat(ItemExportService.slug("")).isEqualTo("collection");
        assertThat(ItemExportService.slug(null)).isEqualTo("collection");
        assertThat(ItemExportService.slug("../../etc/passwd")).isEqualTo("etc-passwd");
        assertThat(ItemExportService.slug("x".repeat(100))).hasSize(40);
    }

    @Test
    void exportFormatParsingIsCaseInsensitiveAndRejectsUnknownFormats() {
        assertThat(ExportFormat.parse("JSON")).isEqualTo(ExportFormat.JSON);
        assertThat(ExportFormat.parse(" csv ")).isEqualTo(ExportFormat.CSV);
        assertThatThrownBy(() -> ExportFormat.parse("xml")).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getReason()).isEqualTo("INVALID_EXPORT_FORMAT"));
    }
}
