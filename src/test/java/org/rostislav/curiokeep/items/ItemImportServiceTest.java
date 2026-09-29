package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionModuleRepository;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleId;
import org.rostislav.curiokeep.items.api.dto.ImportResult;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.Constraints;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.StateContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemImportServiceTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

    @Mock
    ItemRepository items;
    @Mock
    ItemIdentifierRepository identifiers;
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
    ItemImportService service;

    @BeforeEach
    void setUp() {
        service = new ItemImportService(items, identifiers, collectionModules, moduleDefinitions, modules, access, currentUser, mapper,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(NOW, ZoneOffset.UTC));
        AppUserEntity user = new AppUserEntity();
        user.setId(USER);
        when(currentUser.requireCurrentUser()).thenReturn(user);
    }

    private static FieldContract field(String key, FieldType type, boolean required, Constraints constraints) {
        return new FieldContract(key, key, type, required, false, false, false, 0, true, false,
                null, List.of(), List.of(), constraints, null, List.of(), Map.of());
    }

    private void givenBooksModule() {
        ModuleDefinitionEntity def = new ModuleDefinitionEntity();
        def.setId(BOOKS);
        def.setModuleKey("books");
        CollectionModuleEntity enabled = new CollectionModuleEntity();
        enabled.setId(new CollectionModuleId(COLLECTION, BOOKS));
        when(collectionModules.findAllByIdCollectionId(COLLECTION)).thenReturn(List.of(enabled));
        when(moduleDefinitions.findAllById(anyList())).thenReturn(List.of(def));
        when(modules.getContract(def)).thenReturn(new ModuleContract("books", "1.0.0", "Books", null, null,
                List.of(new StateContract("OWNED", "Owned", 1, true, false, Map.of()), new StateContract("WISHLIST", "Wishlist", 2, true, false, Map.of())),
                List.of(),
                List.of(field("title", FieldType.TEXT, true, null), field("pages", FieldType.NUMBER, false, new Constraints(1.0, 5000.0, null, null, null, null, null))),
                List.of(), Map.of()));
    }

    /** saveAll gives the entities ids the way the database would, and remembers what it saved. */
    private List<ItemEntity> captureSavedItems() {
        List<ItemEntity> saved = new ArrayList<>();
        when(items.saveAll(anyList())).thenAnswer(inv -> {
            List<ItemEntity> batch = inv.getArgument(0);
            batch.forEach(item -> {
                item.setId(UUID.randomUUID());
                saved.add(item);
            });
            return batch;
        });
        return saved;
    }

    private static String file(String... entries) {
        return "{\"format\":\"curiokeep-export\",\"version\":1,\"items\":[" + String.join(",", entries) + "]}";
    }

    private static String entry(String attributes) {
        return "{\"module\":\"books\",\"state\":\"OWNED\",\"attributes\":" + attributes + "}";
    }

    private ImportResult run(String json) {
        return service.importJson(COLLECTION, json.getBytes(StandardCharsets.UTF_8));
    }

    private void assertRejected(String json, String reason) {
        assertThatThrownBy(() -> run(json)).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo(reason);
        });
    }

    @Test
    void importsTheValidItemsAndReportsTheOthersWithTheirPosition() {
        givenBooksModule();
        List<ItemEntity> saved = captureSavedItems();
        String json = file(
                entry("{\"title\":\"Dune\",\"pages\":412}"),
                entry("{\"title\":\"Bad pages\",\"pages\":\"many\"}"),
                "{\"module\":\"comics\",\"state\":\"OWNED\",\"attributes\":{\"title\":\"Hulk\"}}",
                entry("{\"title\":\"Neuromancer\"}"),
                "\"not an object\"",
                "{\"attributes\":{\"title\":\"No module\"}}");

        ImportResult result = run(json);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(4);
        assertThat(result.errors()).extracting(ImportResult.ItemError::index).containsExactly(1, 2, 4, 5);
        assertThat(result.errors()).extracting(ImportResult.ItemError::reason)
                .containsExactly("INVALID_FIELD_pages", "MODULE_NOT_ENABLED", "INVALID_ITEM", "MODULE_REQUIRED");
        assertThat(saved).extracting(ItemEntity::getTitle).containsExactly("Dune", "Neuromancer");
        assertThat(saved).allSatisfy(item -> {
            assertThat(item.getCollectionId()).isEqualTo(COLLECTION);
            assertThat(item.getModuleId()).isEqualTo(BOOKS);
            assertThat(item.getCreatedBy()).isEqualTo(USER);
        });
    }

    @Test
    void needsEditorAccessAndTouchesNothingWithoutIt() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(access).requireRole(COLLECTION, USER, Role.EDITOR);

        assertThatThrownBy(() -> run(file())).isInstanceOf(ResponseStatusException.class);

        verify(items, never()).saveAll(anyList());
        verify(collectionModules, never()).findAllByIdCollectionId(COLLECTION);
    }

    @Test
    void refusesFilesThatAreNotAnExportOfThisApp() {
        assertRejected("{not json", "INVALID_EXPORT_FILE");
        assertRejected("[]", "INVALID_EXPORT_FILE");
        assertRejected("{\"items\":[]}", "INVALID_EXPORT_FILE");
        assertRejected("{\"format\":\"something-else\",\"version\":1,\"items\":[]}", "INVALID_EXPORT_FILE");
        assertRejected("{\"format\":\"curiokeep-export\",\"version\":2,\"items\":[]}", "UNSUPPORTED_EXPORT_VERSION");
        assertRejected("{\"format\":\"curiokeep-export\",\"version\":1,\"items\":{}}", "INVALID_EXPORT_FILE");
    }

    @Test
    void refusesMoreItemsThanTheLimitBeforeProcessingAny() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i <= ItemImportService.MAX_ITEMS; i++) many.add("{}");

        assertRejected(file(many.toArray(String[]::new)), "TOO_MANY_ITEMS");

        verify(items, never()).saveAll(anyList());
    }

    @Test
    void leavesOutACoverPathFromAnotherServerButKeepsAnExternalUrl() {
        givenBooksModule();
        List<ItemEntity> saved = captureSavedItems();

        run(file(
                entry("{\"title\":\"Local\",\"providerImageUrl\":\"/api/assets/0123456789abcdef0123456789abcdef.png\"}"),
                entry("{\"title\":\"Remote\",\"providerImageUrl\":\"https://example.com/c.jpg\"}")));

        assertThat(saved.get(0).getAttributes()).doesNotContain("providerImageUrl");
        assertThat(saved.get(1).getAttributes()).contains("https://example.com/c.jpg");
        assertThat(saved).allSatisfy(item -> assertThat(item.getImageName()).isNull());
    }

    @Test
    void stateDefaultsToTheModulesFirstAndAnUnknownOneIsReported() {
        givenBooksModule();
        List<ItemEntity> saved = captureSavedItems();

        ImportResult result = run(file(
                "{\"module\":\"books\",\"attributes\":{\"title\":\"No state\"}}",
                "{\"module\":\"books\",\"state\":\"wishlist\",\"attributes\":{\"title\":\"Lower case\"}}",
                "{\"module\":\"books\",\"state\":\"STOLEN\",\"attributes\":{\"title\":\"Bad state\"}}"));

        assertThat(saved).extracting(ItemEntity::getStateKey).containsExactly("OWNED", "WISHLIST");
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.index()).isEqualTo(2);
            assertThat(error.reason()).isEqualTo("INVALID_STATE");
        });
    }

    @Test
    void titleComesFromTheFileOrFallsBackToTheTitleAttribute() {
        givenBooksModule();
        List<ItemEntity> saved = captureSavedItems();

        run(file(
                "{\"module\":\"books\",\"title\":\"Explicit\",\"attributes\":{\"title\":\"Attribute\"}}",
                "{\"module\":\"books\",\"attributes\":{\"title\":\"Attribute only\"}}"));

        assertThat(saved).extracting(ItemEntity::getTitle).containsExactly("Explicit", "Attribute only");
    }

    @Test
    void keepsTheOriginalCreationTimeButNotOneFromTheFuture() {
        givenBooksModule();
        List<ItemEntity> saved = captureSavedItems();

        run(file(
                "{\"module\":\"books\",\"createdAt\":\"2020-05-17T08:00:00Z\",\"attributes\":{\"title\":\"Old\"}}",
                "{\"module\":\"books\",\"createdAt\":\"2999-01-01T00:00:00Z\",\"attributes\":{\"title\":\"Future\"}}",
                "{\"module\":\"books\",\"createdAt\":\"yesterday-ish\",\"attributes\":{\"title\":\"Garbage\"}}",
                "{\"module\":\"books\",\"attributes\":{\"title\":\"Missing\"}}"));

        OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
        assertThat(saved.get(0).getCreatedAt().toInstant()).isEqualTo(Instant.parse("2020-05-17T08:00:00Z"));
        assertThat(saved.get(0).getUpdatedAt()).isEqualTo(saved.get(0).getCreatedAt());
        assertThat(saved.get(1).getCreatedAt().toInstant()).isEqualTo(now.toInstant());
        assertThat(saved.get(2).getCreatedAt().toInstant()).isEqualTo(now.toInstant());
        assertThat(saved.get(3).getCreatedAt().toInstant()).isEqualTo(now.toInstant());
    }

    @Test
    void storesIdentifiersOnePerTypeAndReportsABadOne() {
        givenBooksModule();
        captureSavedItems();
        ArgumentCaptor<List<ItemIdentifierEntity>> stored = ArgumentCaptor.forClass(List.class);

        ImportResult result = run(file(
                "{\"module\":\"books\",\"attributes\":{\"title\":\"A\"},\"identifiers\":[{\"type\":\"ISBN13\",\"value\":\"1\"},{\"type\":\"ISBN13\",\"value\":\" 2 \"},{\"type\":\"UPC\",\"value\":\"3\"}]}",
                "{\"module\":\"books\",\"attributes\":{\"title\":\"B\"},\"identifiers\":[{\"type\":\"NOPE\",\"value\":\"1\"}]}",
                "{\"module\":\"books\",\"attributes\":{\"title\":\"C\"},\"identifiers\":[{\"type\":\"EAN\",\"value\":\"  \"}]}"));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).extracting(ImportResult.ItemError::reason).containsExactly("INVALID_IDENTIFIER", "INVALID_IDENTIFIER");
        verify(identifiers).saveAll(stored.capture());
        assertThat(stored.getValue()).extracting(ItemIdentifierEntity::getIdValue).containsExactly("2", "3");
        assertThat(stored.getValue()).allSatisfy(id -> assertThat(id.getItemId()).isNotNull());
    }

    @Test
    void storesInBatchesOfTwoHundred() {
        givenBooksModule();
        captureSavedItems();
        String[] entries = new String[450];
        for (int i = 0; i < entries.length; i++) entries[i] = entry("{\"title\":\"Item " + i + "\"}");

        ImportResult result = run(file(entries));

        assertThat(result.imported()).isEqualTo(450);
        ArgumentCaptor<List<ItemEntity>> batches = ArgumentCaptor.forClass(List.class);
        verify(items, times(3)).saveAll(batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(200, 200, 50);
    }

    @Test
    void aBatchThatFailsToSaveIsReportedAndTheNextOneStillRuns() {
        givenBooksModule();
        when(items.saveAll(anyList()))
                .thenThrow(new IllegalStateException("database down"))
                .thenAnswer(inv -> inv.getArgument(0));
        String[] entries = new String[205];
        for (int i = 0; i < entries.length; i++) entries[i] = entry("{\"title\":\"Item " + i + "\"}");

        ImportResult result = run(file(entries));

        assertThat(result.imported()).isEqualTo(5);
        assertThat(result.failed()).isEqualTo(200);
        assertThat(result.errors()).hasSize(ItemImportService.MAX_REPORTED_ERRORS);
        assertThat(result.errors()).allSatisfy(error -> assertThat(error.reason()).isEqualTo("SAVE_FAILED"));
    }

    @Test
    void theReportedErrorsAreCappedButTheCountIsExact() {
        givenBooksModule();
        String[] entries = new String[120];
        for (int i = 0; i < entries.length; i++) entries[i] = "{\"module\":\"nope\"}";

        ImportResult result = run(file(entries));

        assertThat(result.failed()).isEqualTo(120);
        assertThat(result.errors()).hasSize(ItemImportService.MAX_REPORTED_ERRORS);
        assertThat(result.imported()).isZero();
    }

    @Test
    void anEmptyFileImportsNothingAndSucceeds() {
        givenBooksModule();

        ImportResult result = run(file());

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.errors()).isEmpty();
    }
}
