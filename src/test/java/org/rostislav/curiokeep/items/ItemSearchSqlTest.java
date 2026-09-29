package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.ItemQuery.FieldFilter;
import org.rostislav.curiokeep.items.ItemQuery.FilterOperator;
import org.rostislav.curiokeep.items.ItemQuery.Sort;
import org.rostislav.curiokeep.items.ItemQuery.SortTarget;
import org.rostislav.curiokeep.modules.contract.FieldType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItemSearchSqlTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String HOSTILE = "x'; DROP TABLE item; --";

    private static ItemQuery query(String search, List<String> searchKeys, List<String> states, List<FieldFilter> filters, Sort sort) {
        return new ItemQuery(COLLECTION, MODULE, search, searchKeys, states, filters, sort, 0, 25);
    }

    @Test
    void alwaysScopesToTheCollectionAndModule() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(), Sort.newestFirst()));

        assertThat(sql.where()).isEqualTo("i.collection_id = :collectionId AND i.module_id = :moduleId");
        assertThat(sql.whereParams()).containsEntry("collectionId", COLLECTION).containsEntry("moduleId", MODULE);
    }

    @Test
    void requestTextNeverAppearsInTheSqlOnlyInBoundParameters() {
        ItemSearchSql sql = new ItemSearchSql(query(HOSTILE, List.of("title"), List.of("OWNED"), List.of(
                new FieldFilter("pub'lisher", FieldType.TEXT, FilterOperator.CONTAINS, List.of(HOSTILE)),
                new FieldFilter("format", FieldType.ENUM, FilterOperator.IN, List.of(HOSTILE, "B")),
                new FieldFilter("published", FieldType.DATE, FilterOperator.FROM, List.of("2000")),
                new FieldFilter("pages", FieldType.NUMBER, FilterOperator.GTE, List.of("10"))),
                new Sort(SortTarget.FIELD, "so'rt", FieldType.TEXT, false)));

        String all = sql.where() + " " + sql.orderBy();

        assertThat(all).doesNotContain("DROP").doesNotContain("pub'lisher").doesNotContain("so'rt").doesNotContain("2000");
        assertThat(sql.whereParams().values().toString()).contains("DROP");
        assertThat(sql.whereParams()).containsValue("pub'lisher");
        assertThat(sql.orderParams()).containsValue("so'rt");
    }

    @Test
    void searchCoversTheTitleAndEverySearchableFieldCaseInsensitively() {
        ItemSearchSql sql = new ItemSearchSql(query("Dune", List.of("authors", "series"), List.of(), List.of(), Sort.newestFirst()));

        assertThat(sql.where()).contains("lower(i.title) LIKE :search ESCAPE '!'");
        assertThat(sql.where()).contains("jsonb_extract_path_text(i.attributes, :k0)").contains(":k1");
        assertThat(sql.whereParams()).containsEntry("search", "%dune%").containsEntry("k0", "authors").containsEntry("k1", "series");
    }

    @Test
    void likeWildcardsInTheSearchTextAreEscapedSoTheyMatchLiterally() {
        assertThat(ItemSearchSql.likePattern("100%_Pure!")).isEqualTo("%100!%!_pure!!%");
    }

    @Test
    void hasChecksThatTheValueExistsAndIsNotEmpty() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(
                new FieldFilter("old_authors", FieldType.TEXT, FilterOperator.HAS, List.of("true"))), Sort.newestFirst()));

        assertThat(sql.where()).contains("jsonb_typeof(jsonb_extract_path(i.attributes, :k0)) IS NULL")
                .contains("= 'null' THEN FALSE").contains("<> ''").contains("jsonb_array_length(");
        assertThat(sql.where()).doesNotContain("old_authors");
        assertThat(sql.whereParams()).containsEntry("k0", "old_authors");
    }

    @Test
    void stateFilterUsesAnInList() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of("OWNED", "WISHLIST"), List.of(), Sort.newestFirst()));

        assertThat(sql.where()).contains("i.state_key IN (:states)");
        assertThat(sql.whereParams()).containsEntry("states", List.of("OWNED", "WISHLIST"));
    }

    @Test
    void numberFiltersOnlyLookAtRealJsonNumbers() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(
                new FieldFilter("pages", FieldType.NUMBER, FilterOperator.LTE, List.of("300.5"))), Sort.newestFirst()));

        assertThat(sql.where()).contains("jsonb_typeof(jsonb_extract_path(i.attributes, :k0)) = 'number'").contains("<= :n1");
        assertThat(sql.whereParams()).containsEntry("n1", new java.math.BigDecimal("300.5"));
    }

    @Test
    void everyOrderEndsWithTheStableTieBreaker() {
        for (SortTarget target : new SortTarget[]{SortTarget.CREATED_AT, SortTarget.UPDATED_AT, SortTarget.TITLE}) {
            ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(), new Sort(target, null, null, false)));
            assertThat(sql.orderBy()).endsWith("i.created_at DESC, i.id");
        }
        ItemSearchSql byField = new ItemSearchSql(query(null, List.of(), List.of(), List.of(), new Sort(SortTarget.FIELD, "pages", FieldType.NUMBER, true)));
        assertThat(byField.orderBy()).contains("DESC NULLS LAST").contains("CASE WHEN").endsWith("i.created_at DESC, i.id");
        assertThat(byField.orderParams()).containsEntry("sortKey", "pages");
    }

    @Test
    void textFieldsSortCaseInsensitivelyWithEmptyValuesLast() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(), new Sort(SortTarget.FIELD, "publisher", FieldType.TEXT, false)));

        assertThat(sql.orderBy()).startsWith("lower(jsonb_extract_path_text(i.attributes, :sortKey)) ASC NULLS LAST");
    }

    @Test
    void theCountQueryDoesNotNeedTheOrderParameters() {
        ItemSearchSql sql = new ItemSearchSql(query(null, List.of(), List.of(), List.of(), new Sort(SortTarget.FIELD, "pages", FieldType.NUMBER, false)));

        assertThat(sql.whereParams()).doesNotContainKey("sortKey");
        assertThat(sql.where()).doesNotContain("sortKey");
    }
}
