package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.ItemQuery.FilterOperator;
import org.rostislav.curiokeep.items.ItemQuery.SortTarget;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.StateContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemQueryParserTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ModuleContract contract = new ModuleContract(
            "books", "1.0.0", "Books", null, null,
            List.of(state("OWNED"), state("WISHLIST")),
            List.of(),
            List.of(
                    field("title", FieldType.TEXT, true, false, true, true),
                    field("publisher", FieldType.TEXT, false, true, false, true),
                    field("pages", FieldType.NUMBER, false, true, true, true),
                    field("published", FieldType.DATE, false, true, true, true),
                    field("format", FieldType.ENUM, false, true, false, true),
                    field("notes", FieldType.TEXT, false, false, false, true),
                    field("old_field", FieldType.TEXT, true, true, true, false)),
            List.of(),
            Map.of());

    private static StateContract state(String key) {
        return new StateContract(key, key, 1, true, false, Map.of());
    }

    private static FieldContract field(String key, FieldType type, boolean searchable, boolean filterable, boolean sortable, boolean active) {
        return new FieldContract(key, key, type, false, searchable, filterable, sortable, 0, active, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
    }

    private ItemQuery parse(String search, String state, String sort, int page, int size, Map<String, String> other) {
        return ItemQueryParser.parse(contract, new ItemQueryParser.Params(COLLECTION, MODULE, search, state, sort, page, size, other));
    }

    private ItemQuery parse(Map<String, String> other) {
        return parse(null, null, null, 0, 25, other);
    }

    private void assertRejected(Runnable action, String reason) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo(reason);
        });
    }

    @Test
    void withNoOptionsListsNewestFirstWithoutFilters() {
        ItemQuery query = parse(null, null, null, 0, 25, Map.of());

        assertThat(query.sort().target()).isEqualTo(SortTarget.CREATED_AT);
        assertThat(query.sort().descending()).isTrue();
        assertThat(query.search()).isNull();
        assertThat(query.states()).isEmpty();
        assertThat(query.filters()).isEmpty();
        assertThat(query.offset()).isZero();
    }

    @Test
    void offsetIsPageTimesSize() {
        assertThat(parse(null, null, null, 3, 20, Map.of()).offset()).isEqualTo(60);
    }

    @Test
    void rejectsPagingOutsideTheAllowedRange() {
        assertRejected(() -> parse(null, null, null, -1, 25, Map.of()), "INVALID_PAGING");
        assertRejected(() -> parse(null, null, null, 0, 0, Map.of()), "INVALID_PAGING");
        assertRejected(() -> parse(null, null, null, 0, ItemQueryParser.MAX_PAGE_SIZE + 1, Map.of()), "INVALID_PAGING");
        assertThat(parse(null, null, null, 0, ItemQueryParser.MAX_PAGE_SIZE, Map.of()).size()).isEqualTo(100);
    }

    @Test
    void searchIsTrimmedAndBoundedAndUsesOnlyActiveSearchableFields() {
        ItemQuery query = parse("  dune  ", null, null, 0, 25, Map.of());

        assertThat(query.search()).isEqualTo("dune");
        assertThat(query.searchFieldKeys()).containsExactly("title");
        assertThat(parse("   ", null, null, 0, 25, Map.of()).search()).isNull();
        assertRejected(() -> parse("x".repeat(ItemQueryParser.MAX_SEARCH_LENGTH + 1), null, null, 0, 25, Map.of()), "SEARCH_TOO_LONG");
    }

    @Test
    void statesAreUppercasedAndMustBeDeclaredByTheModule() {
        assertThat(parse(null, "owned, wishlist", null, 0, 25, Map.of()).states()).containsExactly("OWNED", "WISHLIST");
        assertRejected(() -> parse(null, "OWNED,STOLEN", null, 0, 25, Map.of()), "INVALID_STATE");
    }

    @Test
    void sortsByBuiltInColumnsAndSortableFieldsOnly() {
        assertThat(parse(null, null, "title,desc", 0, 25, Map.of()).sort().target()).isEqualTo(SortTarget.TITLE);
        assertThat(parse(null, null, "updatedAt", 0, 25, Map.of()).sort().descending()).isFalse();
        ItemQuery byPages = parse(null, null, "pages,desc", 0, 25, Map.of());
        assertThat(byPages.sort().target()).isEqualTo(SortTarget.FIELD);
        assertThat(byPages.sort().fieldKey()).isEqualTo("pages");
        assertThat(byPages.sort().fieldType()).isEqualTo(FieldType.NUMBER);

        assertRejected(() -> parse(null, null, "publisher,asc", 0, 25, Map.of()), "INVALID_SORT"); // not sortable
        assertRejected(() -> parse(null, null, "old_field", 0, 25, Map.of()), "INVALID_SORT"); // inactive
        assertRejected(() -> parse(null, null, "nonsense", 0, 25, Map.of()), "INVALID_SORT");
        assertRejected(() -> parse(null, null, "title,sideways", 0, 25, Map.of()), "INVALID_SORT");
    }

    @Test
    void readsFieldFiltersFromDottedParameters() {
        Map<String, String> other = new LinkedHashMap<>();
        other.put("format.in", "HARDCOVER, PAPERBACK");
        other.put("pages.gte", "100");
        other.put("pages.lte", "5e2");
        other.put("published.from", "1999");
        other.put("published.to", "2001-05");
        other.put("publisher.contains", "ace");
        other.put("moduleId", "ignored-because-it-has-no-dot");

        List<ItemQuery.FieldFilter> filters = parse(other).filters();

        assertThat(filters).hasSize(6);
        assertThat(filters).filteredOn(f -> f.fieldKey().equals("format")).singleElement()
                .satisfies(f -> {
                    assertThat(f.operator()).isEqualTo(FilterOperator.IN);
                    assertThat(f.values()).containsExactly("HARDCOVER", "PAPERBACK");
                });
        assertThat(filters).extracting(ItemQuery.FieldFilter::operator)
                .contains(FilterOperator.GTE, FilterOperator.LTE, FilterOperator.FROM, FilterOperator.TO, FilterOperator.CONTAINS);
    }

    @Test
    void rejectsFiltersOnUnknownNonFilterableOrInactiveFields() {
        assertRejected(() -> parse(Map.of("nope.in", "x")), "INVALID_FILTER_FIELD");
        assertRejected(() -> parse(Map.of("notes.contains", "x")), "INVALID_FILTER_FIELD"); // not filterable
        assertRejected(() -> parse(Map.of("old_field.contains", "x")), "INVALID_FILTER_FIELD"); // inactive
    }

    @Test
    void rejectsAnOperatorThatDoesNotApplyToTheFieldType() {
        assertRejected(() -> parse(Map.of("pages.contains", "1")), "INVALID_FILTER_OPERATOR");
        assertRejected(() -> parse(Map.of("publisher.gte", "1")), "INVALID_FILTER_OPERATOR");
        assertRejected(() -> parse(Map.of("format.from", "2000")), "INVALID_FILTER_OPERATOR");
        assertRejected(() -> parse(Map.of("publisher.matches", "x")), "INVALID_FILTER_OPERATOR");
    }

    @Test
    void hasFindsItemsWithAValueInAnyDeclaredFieldEvenOneThatIsNotFilterableOrIsRetired() {
        List<ItemQuery.FieldFilter> filters = parse(Map.of("notes.has", "true", "old_field.has", "true", "pages.has", "true")).filters();

        assertThat(filters).extracting(ItemQuery.FieldFilter::fieldKey).containsExactlyInAnyOrder("notes", "old_field", "pages");
        assertThat(filters).allSatisfy(filter -> {
            assertThat(filter.operator()).isEqualTo(FilterOperator.HAS);
            assertThat(filter.values()).containsExactly("true");
        });
    }

    @Test
    void hasStillRefusesAFieldTheModuleDoesNotDeclareAndAnyValueButTrue() {
        assertRejected(() -> parse(Map.of("ghost.has", "true")), "INVALID_FILTER_FIELD");
        assertRejected(() -> parse(Map.of("notes.has", "false")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("notes.has", "")), "INVALID_FILTER_VALUE");
    }

    @Test
    void otherOperatorsStillNeedTheFieldToBeActiveAndFilterable() {
        assertRejected(() -> parse(Map.of("notes.contains", "x")), "INVALID_FILTER_FIELD");
        assertRejected(() -> parse(Map.of("old_field.contains", "x")), "INVALID_FILTER_FIELD");
    }

    @Test
    void rejectsMalformedFilterValues() {
        assertRejected(() -> parse(Map.of("pages.gte", "many")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("published.from", "last year")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("published.to", "2020-13-1")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("format.in", "  ")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("format.in", ",,")), "INVALID_FILTER_VALUE");
        assertRejected(() -> parse(Map.of("publisher.contains", "x".repeat(201))), "INVALID_FILTER_VALUE");
        String tooMany = String.join(",", java.util.Collections.nCopies(21, "a"));
        assertRejected(() -> parse(Map.of("format.in", tooMany)), "INVALID_FILTER_VALUE");
    }

    @Test
    void limitsHowManyFiltersOneRequestMayCarry() {
        List<FieldContract> fields = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) fields.add(field("f" + i, FieldType.TEXT, false, true, false, true));
        ModuleContract wide = new ModuleContract("wide", "1.0.0", "Wide", null, null, List.of(state("OWNED")), List.of(), fields, List.of(), Map.of());
        Map<String, String> ten = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) ten.put("f" + i + ".contains", "a");
        Map<String, String> eleven = new LinkedHashMap<>(ten);
        eleven.put("f10.contains", "a");

        assertThat(ItemQueryParser.parse(wide, new ItemQueryParser.Params(COLLECTION, MODULE, null, null, null, 0, 25, ten)).filters()).hasSize(10);
        assertRejected(() -> ItemQueryParser.parse(wide, new ItemQueryParser.Params(COLLECTION, MODULE, null, null, null, 0, 25, eleven)), "TOO_MANY_FILTERS");
    }
}
