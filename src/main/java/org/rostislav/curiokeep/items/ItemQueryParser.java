package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.items.ItemQuery.FieldFilter;
import org.rostislav.curiokeep.items.ItemQuery.FilterOperator;
import org.rostislav.curiokeep.items.ItemQuery.Sort;
import org.rostislav.curiokeep.items.ItemQuery.SortTarget;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Turns the raw list parameters into an {@link ItemQuery}, accepting only what the module contract allows: filters on
 * {@code filterable} fields, sorting on {@code sortable} fields, and free-text search over {@code searchable} fields.
 * <p>
 * Field filters arrive as {@code <fieldKey>.<operator>=<value>} (operators: in, contains, gte, lte, from, to).
 */
final class ItemQueryParser {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_SEARCH_LENGTH = 100;
    private static final int MAX_FILTERS = 10;
    private static final int MAX_VALUES = 20;
    private static final int MAX_VALUE_LENGTH = 200;
    private static final Pattern PARTIAL_ISO_DATE = Pattern.compile("\\d{4}(-\\d{2}(-\\d{2})?)?");

    private ItemQueryParser() {
    }

    /** The parameters that are not field filters; anything else with a dot in its name is read as one. */
    record Params(UUID collectionId, UUID moduleId, String search, String state, String sort, int page, int size,
                  Map<String, String> other) {
    }

    static ItemQuery parse(ModuleContract contract, Params params) {
        if (params.page() < 0 || params.size() < 1 || params.size() > MAX_PAGE_SIZE) {
            throw badRequest("INVALID_PAGING");
        }
        return new ItemQuery(
                params.collectionId(),
                params.moduleId(),
                parseSearch(params.search()),
                contract.fields().stream().filter(f -> f.active() && f.searchable()).map(FieldContract::key).toList(),
                parseStates(contract, params.state()),
                parseFilters(contract, params.other()),
                parseSort(contract, params.sort()),
                params.page(),
                params.size());
    }

    private static String parseSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String trimmed = search.trim();
        if (trimmed.length() > MAX_SEARCH_LENGTH) throw badRequest("SEARCH_TOO_LONG");
        return trimmed;
    }

    private static List<String> parseStates(ModuleContract contract, String state) {
        if (state == null || state.isBlank()) return List.of();
        List<String> requested = Arrays.stream(state.split(",")).map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isEmpty()).toList();
        for (String key : requested) {
            if (contract.states().stream().noneMatch(s -> s.key().equalsIgnoreCase(key))) throw badRequest("INVALID_STATE");
        }
        return requested;
    }

    private static Sort parseSort(ModuleContract contract, String sort) {
        if (sort == null || sort.isBlank()) return Sort.newestFirst();
        String[] parts = sort.split(",", 2);
        String key = parts[0].trim();
        boolean descending = false;
        if (parts.length == 2) {
            switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
                case "asc" -> descending = false;
                case "desc" -> descending = true;
                default -> throw badRequest("INVALID_SORT");
            }
        }
        switch (key) {
            case "createdAt" -> {
                return new Sort(SortTarget.CREATED_AT, null, null, descending);
            }
            case "updatedAt" -> {
                return new Sort(SortTarget.UPDATED_AT, null, null, descending);
            }
            case "title" -> {
                return new Sort(SortTarget.TITLE, null, null, descending);
            }
            default -> {
                FieldContract field = contract.fields().stream()
                        .filter(f -> f.active() && f.sortable() && f.key().equals(key)).findFirst()
                        .orElseThrow(() -> badRequest("INVALID_SORT"));
                return new Sort(SortTarget.FIELD, field.key(), field.type(), descending);
            }
        }
    }

    private static List<FieldFilter> parseFilters(ModuleContract contract, Map<String, String> other) {
        List<FieldFilter> filters = new ArrayList<>();
        for (Map.Entry<String, String> entry : other.entrySet()) {
            int dot = entry.getKey().lastIndexOf('.');
            if (dot <= 0) continue; // not a filter parameter
            if (filters.size() >= MAX_FILTERS) throw badRequest("TOO_MANY_FILTERS");
            String fieldKey = entry.getKey().substring(0, dot);
            FilterOperator operator = operator(entry.getKey().substring(dot + 1));
            // "has" asks whether a value exists, which is how items in a deprecated field are found, so it works on any declared field.
            FieldContract field = contract.fields().stream()
                    .filter(f -> f.key().equals(fieldKey) && (operator == FilterOperator.HAS || (f.active() && f.filterable()))).findFirst()
                    .orElseThrow(() -> badRequest("INVALID_FILTER_FIELD"));
            filters.add(new FieldFilter(field.key(), field.type(), operator, values(field.type(), operator, entry.getValue())));
        }
        return filters;
    }

    private static FilterOperator operator(String name) {
        return switch (name) {
            case "in" -> FilterOperator.IN;
            case "contains" -> FilterOperator.CONTAINS;
            case "gte" -> FilterOperator.GTE;
            case "lte" -> FilterOperator.LTE;
            case "from" -> FilterOperator.FROM;
            case "to" -> FilterOperator.TO;
            case "has" -> FilterOperator.HAS;
            default -> throw badRequest("INVALID_FILTER_OPERATOR");
        };
    }

    private static List<String> values(FieldType type, FilterOperator operator, String raw) {
        if (raw == null || raw.isBlank()) throw badRequest("INVALID_FILTER_VALUE");
        boolean applicable = switch (operator) {
            case IN -> type == FieldType.ENUM || type == FieldType.TEXT || type == FieldType.TAGS || type == FieldType.BOOLEAN || type == FieldType.LINK;
            case CONTAINS -> type == FieldType.TEXT || type == FieldType.LINK;
            case GTE, LTE -> type == FieldType.NUMBER;
            case FROM, TO -> type == FieldType.DATE;
            case HAS -> true;
        };
        if (!applicable) throw badRequest("INVALID_FILTER_OPERATOR");

        if (operator == FilterOperator.HAS && !raw.trim().equals("true")) throw badRequest("INVALID_FILTER_VALUE");

        List<String> values = operator == FilterOperator.IN
                ? Arrays.stream(raw.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList()
                : List.of(raw.trim());
        if (values.isEmpty() || values.size() > MAX_VALUES || values.stream().anyMatch(v -> v.length() > MAX_VALUE_LENGTH)) {
            throw badRequest("INVALID_FILTER_VALUE");
        }
        if ((operator == FilterOperator.GTE || operator == FilterOperator.LTE) && parseNumber(values.getFirst()).isEmpty()) {
            throw badRequest("INVALID_FILTER_VALUE");
        }
        if ((operator == FilterOperator.FROM || operator == FilterOperator.TO) && !PARTIAL_ISO_DATE.matcher(values.getFirst()).matches()) {
            throw badRequest("INVALID_FILTER_VALUE");
        }
        return values;
    }

    static Optional<BigDecimal> parseNumber(String value) {
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
