package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.items.ItemQuery.FieldFilter;
import org.rostislav.curiokeep.items.ItemQuery.Sort;
import org.rostislav.curiokeep.modules.contract.FieldType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the WHERE and ORDER BY parts of the item listing query for PostgreSQL.
 * <p>
 * The SQL text is assembled only from fixed fragments. Everything that comes from the request (search text, field keys,
 * filter values) is passed as a bound parameter, and the field keys and operators were validated against the module
 * contract by {@link ItemQueryParser} before this class sees them.
 */
final class ItemSearchSql {

    /** Escape character for LIKE patterns; '!' avoids backslash handling differences between the driver and Hibernate. */
    private static final char LIKE_ESCAPE = '!';
    private static final String ATTR = "i.attributes";

    private final ItemQuery query;
    private final Map<String, Object> whereParams = new LinkedHashMap<>();
    private final Map<String, Object> orderParams = new LinkedHashMap<>();
    private final List<String> conditions = new ArrayList<>();
    private int counter;

    ItemSearchSql(ItemQuery query) {
        this.query = query;
        conditions.add("i.collection_id = :collectionId");
        conditions.add("i.module_id = :moduleId");
        whereParams.put("collectionId", query.collectionId());
        whereParams.put("moduleId", query.moduleId());
        addSearch();
        addStates();
        query.filters().forEach(this::addFilter);
    }

    String where() {
        return String.join(" AND ", conditions);
    }

    Map<String, Object> whereParams() {
        return whereParams;
    }

    /** Always ends with created_at and id so that pages are stable when the chosen sort has ties. */
    String orderBy() {
        Sort sort = query.sort();
        String direction = sort.descending() ? "DESC" : "ASC";
        String primary = switch (sort.target()) {
            case CREATED_AT -> "i.created_at " + direction;
            case UPDATED_AT -> "i.updated_at " + direction;
            case TITLE -> "lower(i.title) " + direction + " NULLS LAST";
            case FIELD -> {
                String key = bindOrder("sortKey", sort.fieldKey());
                yield (sort.fieldType() == FieldType.NUMBER ? numberExpression(key) : "lower(" + text(key) + ")") + " " + direction + " NULLS LAST";
            }
        };
        return primary + ", i.created_at DESC, i.id";
    }

    Map<String, Object> orderParams() {
        return orderParams;
    }

    static String likePattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), LIKE_ESCAPE + "" + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }

    private void addSearch() {
        if (query.search() == null) return;
        whereParams.put("search", likePattern(query.search()));
        List<String> alternatives = new ArrayList<>();
        alternatives.add("lower(i.title) LIKE :search ESCAPE '" + LIKE_ESCAPE + "'");
        for (String key : query.searchFieldKeys()) {
            alternatives.add("lower(" + text(bind("k", key)) + ") LIKE :search ESCAPE '" + LIKE_ESCAPE + "'");
        }
        conditions.add("(" + String.join(" OR ", alternatives) + ")");
    }

    private void addStates() {
        if (query.states().isEmpty()) return;
        whereParams.put("states", query.states());
        conditions.add("i.state_key IN (:states)");
    }

    private void addFilter(FieldFilter filter) {
        String key = bind("k", filter.fieldKey());
        switch (filter.operator()) {
            case IN -> {
                String values = bind("v", filter.values());
                conditions.add("(" + text(key) + " IN (:" + values + ") OR CASE WHEN jsonb_typeof(" + json(key) + ") = 'array' "
                        + "THEN EXISTS (SELECT 1 FROM jsonb_array_elements_text(" + json(key) + ") AS e(v) WHERE e.v IN (:" + values + ")) "
                        + "ELSE FALSE END)");
            }
            case CONTAINS -> {
                String pattern = bind("p", likePattern(filter.values().getFirst()));
                conditions.add("lower(" + text(key) + ") LIKE :" + pattern + " ESCAPE '" + LIKE_ESCAPE + "'");
            }
            case GTE, LTE -> {
                String number = bind("n", ItemQueryParser.parseNumber(filter.values().getFirst()).orElseThrow());
                conditions.add(numberExpression(key) + (filter.operator() == ItemQuery.FilterOperator.GTE ? " >= :" : " <= :") + number);
            }
            case FROM, TO -> {
                String date = bind("d", filter.values().getFirst());
                String prefix = "substr(" + text(key) + ", 1, length(CAST(:" + date + " AS text)))";
                conditions.add(prefix + (filter.operator() == ItemQuery.FilterOperator.FROM ? " >= " : " <= ") + "CAST(:" + date + " AS text)");
            }
        }
    }

    private String text(String keyParam) {
        return "jsonb_extract_path_text(" + ATTR + ", :" + keyParam + ")";
    }

    private String json(String keyParam) {
        return "jsonb_extract_path(" + ATTR + ", :" + keyParam + ")";
    }

    /** A number only when the stored JSON value is one, so a stray string in old data cannot break the query. */
    private String numberExpression(String keyParam) {
        return "(CASE WHEN jsonb_typeof(" + json(keyParam) + ") = 'number' THEN CAST(" + text(keyParam) + " AS numeric) END)";
    }

    private String bind(String prefix, Object value) {
        String name = prefix + counter++;
        whereParams.put(name, value);
        return name;
    }

    private String bindOrder(String name, Object value) {
        orderParams.put(name, value);
        return name;
    }
}
