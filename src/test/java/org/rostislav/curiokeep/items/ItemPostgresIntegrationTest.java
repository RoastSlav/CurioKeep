package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.ItemQueryParser.Params;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.StateContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations on PostgreSQL and the item listing query against real JSONB data. The H2-based tests cannot
 * do either. Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "curiokeep.modules.import-dir=target/it-data/modules",
        "curiokeep.assets.dir=target/it-data/assets"
})
class ItemPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ItemSearchRepository repository;
    @Autowired
    ItemRepository items;
    @Autowired
    ItemExportService exporter;
    @Autowired
    ItemImportService importer;
    @Autowired
    ModuleQueryService modules;
    @Autowired
    ModuleDefinitionRepository moduleRepository;

    private static UUID collectionId;
    private static UUID otherCollectionId;
    private static UUID moduleId;
    private static ModuleContract books;
    private static boolean seeded;

    /** The contract for the DATE, TAGS and BOOLEAN cases, which the bundled books module has no field for. */
    private static final ModuleContract EXTRA = new ModuleContract("extra", "1.0.0", "Extra", null, null,
            List.of(new StateContract("OWNED", "Owned", 1, true, false, Map.of())), List.of(),
            List.of(field("bought", FieldType.DATE), field("labels", FieldType.TAGS), field("signed", FieldType.BOOLEAN)),
            List.of(), Map.of());

    private static FieldContract field(String key, FieldType type) {
        return new FieldContract(key, key, type, false, false, true, true, 0, true, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
    }

    @BeforeEach
    void seedOnce() {
        seed();
    }

    private synchronized void seed() {
        if (seeded) return;
        ModuleDefinitionEntity module = moduleRepository.findByModuleKey("books").orElseThrow();
        moduleId = module.getId();
        books = modules.getContract(module);

        UUID owner = jdbc.queryForObject("INSERT INTO app_user (email, display_name) VALUES ('it@example.test', 'IT') RETURNING id", UUID.class);
        collectionId = jdbc.queryForObject("INSERT INTO collection (owner_user_id, name) VALUES (?, 'Main') RETURNING id", UUID.class, owner);
        otherCollectionId = jdbc.queryForObject("INSERT INTO collection (owner_user_id, name) VALUES (?, 'Other') RETURNING id", UUID.class, owner);

        OffsetDateTime t = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        insert(collectionId, "OWNED", "Dune", "{\"authors\":\"Frank Herbert\",\"publisher\":\"Ace\",\"published_year\":1965,\"format\":\"PAPERBACK\"}", t.plusDays(1));
        insert(collectionId, "OWNED", "Dune Messiah", "{\"authors\":\"Frank Herbert\",\"publisher\":\"Ace\",\"published_year\":1969,\"format\":\"HARDCOVER\"}", t.plusDays(2));
        insert(collectionId, "WISHLIST", "Neuromancer", "{\"authors\":\"William Gibson\",\"publisher\":\"Ace\",\"published_year\":1984,\"format\":\"PAPERBACK\"}", t.plusDays(3));
        insert(collectionId, "OWNED", "100% Pure_Fiction", "{\"authors\":\"Nobody\"}", t.plusDays(4));
        // a stray string where the contract says number: legacy data must not break number queries
        insert(collectionId, "OWNED", "Hyperion", "{\"authors\":\"Dan Simmons\",\"publisher\":\"Bantam\",\"published_year\":\"unknown\"}", t.plusDays(5));
        insert(collectionId, "OWNED", "hyperion rising", "{\"authors\":\"Dan Simmons\"}", t.plusDays(6));
        insert(otherCollectionId, "OWNED", "Dune In Another Collection", "{\"authors\":\"Frank Herbert\"}", t.plusDays(7));
        // thirty items sharing one timestamp: paging must stay stable through the tie
        OffsetDateTime tie = t.plusDays(30);
        for (int i = 0; i < 30; i++) {
            insert(collectionId, "OWNED", String.format("Filler %02d", i), "{\"authors\":\"Filler Author\"}", tie);
        }
        // items for the synthetic contract live in their own collection so they do not disturb the counts above
        UUID extra = jdbc.queryForObject("INSERT INTO collection (owner_user_id, name) VALUES (?, 'Extra') RETURNING id", UUID.class, owner);
        insert(extra, "OWNED", "Old", "{\"bought\":\"2019-12-31\",\"labels\":[\"sci-fi\",\"classic\"],\"signed\":true}", t.plusDays(1));
        insert(extra, "OWNED", "Spring", "{\"bought\":\"2020-05-17\",\"labels\":[\"classic\"],\"signed\":false}", t.plusDays(2));
        insert(extra, "OWNED", "Later", "{\"bought\":\"2021\",\"labels\":\"not-a-list\"}", t.plusDays(3));
        extraCollectionId = extra;
        seeded = true;
    }

    private static UUID extraCollectionId;

    private void insert(UUID collection, String state, String title, String attributes, OffsetDateTime createdAt) {
        jdbc.update("INSERT INTO item (collection_id, module_id, state_key, title, attributes, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?)", collection, moduleId, state, title, attributes, createdAt, createdAt);
    }

    private Page<ItemEntity> query(UUID collection, ModuleContract contract, String search, String state, String sort, int page, int size, Map<String, String> filters) {
        seed();
        return repository.search(ItemQueryParser.parse(contract, new Params(collection, moduleId, search, state, sort, page, size, filters)));
    }

    private List<String> titles(Page<ItemEntity> page) {
        return page.getContent().stream().map(ItemEntity::getTitle).toList();
    }

    private List<String> titles(String search, String state, String sort, Map<String, String> filters) {
        return titles(query(collectionId, books, search, state, sort, 0, 100, filters));
    }

    @Test
    void flywayAppliedEveryMigrationAndHibernateValidatedTheSchema() {
        seed();
        List<String> versions = jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = 'item'", String.class);

        assertThat(versions).containsExactly("1", "2");
        assertThat(indexes).contains("idx_item_collection_module_created", "idx_item_module_state", "gin_item_attributes")
                .doesNotContain("idx_item_collection_module");
    }

    @Test
    void bundledModulesWereLoadedIntoTheRealSchema() {
        seed();
        List<String> keys = jdbc.queryForList("SELECT module_key FROM module_definition ORDER BY module_key", String.class);

        assertThat(keys).contains("books", "comics");
    }

    @Test
    void defaultListingIsNewestFirstAndScopedToTheCollection() {
        List<String> all = titles(null, null, null, Map.of());

        assertThat(all).hasSize(36).doesNotContain("Dune In Another Collection");
        // the thirty fillers share the newest timestamp, so they come first in an order decided by id
        assertThat(all.subList(0, 30)).allMatch(title -> title.startsWith("Filler"));
        assertThat(all.subList(30, 36)).containsExactly("hyperion rising", "Hyperion", "100% Pure_Fiction", "Neuromancer", "Dune Messiah", "Dune");
    }

    @Test
    void pagesDoNotOverlapOrSkipItemsEvenWhenManyShareATimestamp() {
        Set<UUID> seen = new HashSet<>();
        List<UUID> order = new ArrayList<>();
        Page<ItemEntity> page;
        int number = 0;
        do {
            page = query(collectionId, books, null, null, null, number++, 7, Map.of());
            page.getContent().forEach(item -> {
                assertThat(seen.add(item.getId())).as("no item on two pages").isTrue();
                order.add(item.getId());
            });
        } while (page.hasNext());

        assertThat(seen).hasSize(36);
        assertThat(page.getTotalElements()).isEqualTo(36);
    }

    @Test
    void pagingReportsTheTotalOfTheFilteredResultNotThePage() {
        Page<ItemEntity> page = query(collectionId, books, "filler", null, null, 1, 10, Map.of());

        assertThat(page.getTotalElements()).isEqualTo(30);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void searchMatchesTheTitleAndSearchableFieldsIgnoringCase() {
        assertThat(titles("dune", null, "title,asc", Map.of())).containsExactly("Dune", "Dune Messiah");
        assertThat(titles("HERBERT", null, "title,asc", Map.of())).containsExactly("Dune", "Dune Messiah"); // authors is searchable
        assertThat(titles("gibson", null, null, Map.of())).containsExactly("Neuromancer");
        assertThat(titles("nothing-matches-this", null, null, Map.of())).isEmpty();
    }

    @Test
    void searchTreatsPercentAndUnderscoreAsLiteralCharacters() {
        assertThat(titles("%", null, null, Map.of())).containsExactly("100% Pure_Fiction");
        assertThat(titles("_", null, null, Map.of())).containsExactly("100% Pure_Fiction");
        assertThat(titles("!", null, null, Map.of())).isEmpty();
    }

    @Test
    void hostileSearchTextIsJustTextAndTheTableSurvives() {
        assertThat(titles("'; DROP TABLE item; --", null, null, Map.of())).isEmpty();
        assertThat(titles("\\", null, null, Map.of())).isEmpty();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM item", Integer.class)).isGreaterThan(30);
    }

    @Test
    void stateFilterKeepsOnlyThoseStates() {
        assertThat(titles(null, "WISHLIST", null, Map.of())).containsExactly("Neuromancer");
        assertThat(titles(null, "wishlist,lost", null, Map.of())).containsExactly("Neuromancer");
    }

    @Test
    void enumFilterMatchesAnyOfTheGivenValues() {
        assertThat(titles(null, null, "title,asc", Map.of("format.in", "PAPERBACK"))).containsExactly("Dune", "Neuromancer");
        assertThat(titles(null, null, "title,asc", Map.of("format.in", "PAPERBACK,HARDCOVER"))).containsExactly("Dune", "Dune Messiah", "Neuromancer");
    }

    @Test
    void textContainsFilterIgnoresCase() {
        assertThat(titles(null, null, "title,asc", Map.of("publisher.contains", "ACE"))).containsExactly("Dune", "Dune Messiah", "Neuromancer");
        assertThat(titles(null, null, "title,asc", Map.of("publisher.contains", "bant"))).containsExactly("Hyperion");
    }

    @Test
    void numberRangeFilterIgnoresValuesThatAreNotNumbers() {
        assertThat(titles(null, null, "published_year,asc", Map.of("published_year.gte", "1969", "published_year.lte", "1984")))
                .containsExactly("Dune Messiah", "Neuromancer");
        // "Hyperion" holds the string "unknown" there; it must be excluded, not crash the query
        assertThat(titles(null, null, null, Map.of("published_year.gte", "0"))).doesNotContain("Hyperion").hasSize(3);
    }

    @Test
    void sortingByANumberFieldIsNumericAndPutsMissingValuesLast() {
        List<String> ascending = titles(null, null, "published_year,asc", Map.of());
        List<String> descending = titles(null, null, "published_year,desc", Map.of());

        assertThat(ascending.subList(0, 3)).containsExactly("Dune", "Dune Messiah", "Neuromancer");
        assertThat(descending.subList(0, 3)).containsExactly("Neuromancer", "Dune Messiah", "Dune");
        assertThat(ascending).hasSize(36);
        assertThat(descending).hasSize(36);
    }

    @Test
    void sortingByTitleIgnoresCaseAndWorksInBothDirections() {
        List<String> ascending = titles("hyperion", null, "title,asc", Map.of());
        List<String> descending = titles("hyperion", null, "title,desc", Map.of());

        assertThat(ascending).containsExactly("Hyperion", "hyperion rising");
        assertThat(descending).containsExactly("hyperion rising", "Hyperion");
    }

    @Test
    void dateFilterUnderstandsYearMonthAndDayPrefixes() {
        ModuleContract extra = EXTRA;
        seed();
        Page<ItemEntity> may = query(extraCollectionId, extra, null, null, "createdAt,asc", 0, 10, Map.of("bought.from", "2020-05", "bought.to", "2020-05"));
        Page<ItemEntity> year2020on = query(extraCollectionId, extra, null, null, "createdAt,asc", 0, 10, Map.of("bought.from", "2020"));
        Page<ItemEntity> upTo2019 = query(extraCollectionId, extra, null, null, "createdAt,asc", 0, 10, Map.of("bought.to", "2019"));

        assertThat(titles(may)).containsExactly("Spring");
        assertThat(titles(year2020on)).containsExactly("Spring", "Later");
        assertThat(titles(upTo2019)).containsExactly("Old");
    }

    @Test
    void tagFilterMatchesAnElementOfAListAndBooleanFilterMatchesJsonBooleans() {
        seed();
        Page<ItemEntity> classic = query(extraCollectionId, EXTRA, null, null, "createdAt,asc", 0, 10, Map.of("labels.in", "classic"));
        Page<ItemEntity> sciFi = query(extraCollectionId, EXTRA, null, null, "createdAt,asc", 0, 10, Map.of("labels.in", "sci-fi"));
        Page<ItemEntity> signed = query(extraCollectionId, EXTRA, null, null, "createdAt,asc", 0, 10, Map.of("signed.in", "true"));

        assertThat(titles(classic)).containsExactly("Old", "Spring");
        assertThat(titles(sciFi)).containsExactly("Old");
        assertThat(titles(signed)).containsExactly("Old");
    }

    @Test
    void filtersCombineWithAnd() {
        assertThat(titles("dune", "OWNED", "title,asc", Map.of("format.in", "HARDCOVER", "publisher.contains", "ace")))
                .containsExactly("Dune Messiah");
        assertThat(titles("dune", "WISHLIST", null, Map.of())).isEmpty();
    }

    @Test
    void countsGroupTheCollectionsItemsByModuleAndState() {
        Map<String, Long> owned = new java.util.HashMap<>();
        items.countByModuleAndState(collectionId).forEach(row -> {
            assertThat(row.getModuleId()).isEqualTo(moduleId);
            owned.put(row.getStateKey(), row.getCount());
        });

        assertThat(owned).containsEntry("OWNED", 35L).containsEntry("WISHLIST", 1L).hasSize(2);
        assertThat(items.countByModuleAndState(otherCollectionId)).singleElement()
                .satisfies(row -> assertThat(row.getCount()).isEqualTo(1L));
    }

    /** Runs the block as the integration user, which is who the services see as the signed-in user. */
    private <T> T asItUser(java.util.concurrent.Callable<T> action) throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("it@example.test", null, List.of()));
        try {
            return action.call();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    private UUID newCollectionWithBooksModule(String name) {
        UUID owner = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'it@example.test'", UUID.class);
        UUID id = jdbc.queryForObject("INSERT INTO collection (owner_user_id, name) VALUES (?, ?) RETURNING id", UUID.class, owner, name);
        jdbc.update("INSERT INTO collection_member (collection_id, user_id, role) VALUES (?, ?, 'OWNER')", id, owner);
        jdbc.update("INSERT INTO collection_module (collection_id, module_id) VALUES (?, ?)", id, moduleId);
        return id;
    }

    @Test
    void exportingAndImportingACollectionLargerThanOneBatchLosesNothing() throws Exception {
        UUID source = newCollectionWithBooksModule("Export source");
        // groups of three items share a timestamp, so some ties straddle the 500-item batch boundary
        jdbc.update("INSERT INTO item (collection_id, module_id, state_key, title, attributes, created_at, updated_at) "
                + "SELECT ?, ?, CASE WHEN n % 4 = 0 THEN 'WISHLIST' ELSE 'OWNED' END, 'Book ' || n, "
                + "jsonb_build_object('title', 'Book ' || n, 'published_year', n, 'authors', 'Author ' || (n % 7), "
                + "'providerImageUrl', '/api/assets/0123456789abcdef0123456789abcdef.png'), "
                + "timestamptz '2026-01-01 00:00:00+00' + (n / 3) * interval '1 second', timestamptz '2026-01-01 00:00:00+00' + (n / 3) * interval '1 second' "
                + "FROM generate_series(1, 1200) n", source, moduleId);
        jdbc.update("INSERT INTO item_identifier (item_id, id_type, id_value) SELECT id, 'ISBN13', 'isbn-' || title "
                + "FROM item WHERE collection_id = ? AND (attributes ->> 'published_year')::int <= 100", source);

        byte[] exported = asItUser(() -> {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            exporter.open(source, ExportFormat.JSON, null).body().writeTo(out);
            return out.toByteArray();
        });
        tools.jackson.databind.JsonNode root = new tools.jackson.databind.ObjectMapper().readTree(exported);
        Set<String> titlesInFile = new HashSet<>();
        root.path("items").forEach(item -> titlesInFile.add(item.path("title").asString()));

        assertThat(root.path("items")).hasSize(1200);
        assertThat(titlesInFile).as("no item written twice or skipped").hasSize(1200);

        UUID target = newCollectionWithBooksModule("Import target");
        var result = asItUser(() -> importer.importJson(target, exported));

        assertThat(result.imported()).isEqualTo(1200);
        assertThat(result.failed()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item WHERE collection_id = ?", Integer.class, target)).isEqualTo(1200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item i JOIN item_identifier d ON d.item_id = i.id WHERE i.collection_id = ?", Integer.class, target)).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item WHERE collection_id = ? AND state_key = 'WISHLIST'", Integer.class, target)).isEqualTo(300);
        // a cover path from the source server is not carried over
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item WHERE collection_id = ? AND jsonb_exists(attributes, 'providerImageUrl')", Integer.class, target)).isZero();
        // everything else about an item, including when it was added, survives
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM item a JOIN item b ON a.title = b.title AND b.collection_id = ? "
                        + "WHERE a.collection_id = ? AND a.created_at = b.created_at AND a.state_key = b.state_key "
                        + "AND (a.attributes - 'providerImageUrl') = b.attributes", Integer.class, target, source)).isEqualTo(1200);
    }

    @Test
    void csvExportOfAModuleHasOneRowPerItemAndAHeader() throws Exception {
        UUID source = newCollectionWithBooksModule("Csv source");
        insert(source, "OWNED", "=cmd|'/c calc'!A1", "{\"title\":\"=cmd|'/c calc'!A1\",\"published_year\":1999,\"authors\":\"A, B\"}", OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        insert(source, "OWNED", "Plain", "{\"title\":\"Plain\",\"published_year\":-5}", OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));

        String csv = asItUser(() -> {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            exporter.open(source, ExportFormat.CSV, moduleId).body().writeTo(out);
            return out.toString(java.nio.charset.StandardCharsets.UTF_8);
        });
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(3);
        assertThat(lines[0]).startsWith("\uFEFFstate,title,");
        assertThat(lines[1]).contains("'=cmd|").doesNotContain(",=cmd");
        assertThat(lines[1]).contains("\"A, B\"");
        assertThat(lines[2]).contains(",-5,");
    }

    @Test
    void aViewerCanExportButCannotImport() throws Exception {
        UUID collection = newCollectionWithBooksModule("Viewer test");
        UUID viewer = jdbc.queryForObject("INSERT INTO app_user (email, display_name) VALUES ('viewer@example.test', 'V') RETURNING id", UUID.class);
        jdbc.update("INSERT INTO collection_member (collection_id, user_id, role) VALUES (?, ?, 'VIEWER')", collection, viewer);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("viewer@example.test", null, List.of()));
        try {
            assertThat(exporter.open(collection, ExportFormat.JSON, null).fileName()).endsWith(".json");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> importer.importJson(collection, "{}".getBytes()))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void hasMatchesOnlyFieldsThatReallyHoldAValue() {
        seed();
        UUID owner = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'it@example.test'", UUID.class);
        UUID has = jdbc.queryForObject("INSERT INTO collection (owner_user_id, name) VALUES (?, 'Has') RETURNING id", UUID.class, owner);
        OffsetDateTime t = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        insert(has, "OWNED", "text", "{\"old\":\"x\"}", t.plusSeconds(1));
        insert(has, "OWNED", "empty string", "{\"old\":\"\"}", t.plusSeconds(2));
        insert(has, "OWNED", "json null", "{\"old\":null}", t.plusSeconds(3));
        insert(has, "OWNED", "empty list", "{\"old\":[]}", t.plusSeconds(4));
        insert(has, "OWNED", "list", "{\"old\":[\"a\"]}", t.plusSeconds(5));
        insert(has, "OWNED", "zero", "{\"old\":0}", t.plusSeconds(6));
        insert(has, "OWNED", "false", "{\"old\":false}", t.plusSeconds(7));
        insert(has, "OWNED", "absent", "{\"other\":1}", t.plusSeconds(8));
        ModuleContract withOld = new ModuleContract("m", "1.0.0", "M", null, null,
                List.of(new StateContract("OWNED", "Owned", 1, true, false, Map.of())), List.of(),
                List.of(new FieldContract("old", "Old", FieldType.TEXT, false, false, false, false, 0, true, true, null, List.of(), List.of(), null, null, List.of(), Map.of(), null)),
                List.of(), Map.of());

        Page<ItemEntity> page = repository.search(ItemQueryParser.parse(withOld, new Params(has, moduleId, null, null, "createdAt,asc", 0, 100, Map.of("old.has", "true"))));

        assertThat(titles(page)).containsExactly("text", "list", "zero", "false");
        assertThat(repository.count(ItemQueryParser.parse(withOld, new Params(has, moduleId, null, null, null, 0, 1, Map.of("old.has", "true"))))).isEqualTo(4);
    }

    @Test
    void likeWildcardsInAContainsFilterAreLiteral() {
        assertThat(titles(null, null, null, Map.of("publisher.contains", "%"))).isEmpty();
    }
}
