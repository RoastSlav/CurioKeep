package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.modules.contract.FieldType;

import java.util.List;
import java.util.UUID;

/**
 * A validated request to list items. Every field key and operator in it has already been checked against the module contract,
 * so the SQL builder can rely on it; values are never placed in SQL text, only bound as parameters.
 */
record ItemQuery(
        UUID collectionId,
        UUID moduleId,
        String search,
        List<String> searchFieldKeys,
        List<String> states,
        List<FieldFilter> filters,
        Sort sort,
        int page,
        int size
) {

    enum FilterOperator {
        /** The value equals one of the given values, or is a list that contains one of them. */
        IN,
        /** Case-insensitive substring. */
        CONTAINS,
        GTE,
        LTE,
        FROM,
        TO,
        /** The field holds a value: present, not null, not an empty string or list. Works on any declared field. */
        HAS
    }

    /** {@code values} holds one entry for every operator except {@link FilterOperator#IN}. */
    record FieldFilter(String fieldKey, FieldType fieldType, FilterOperator operator, List<String> values) {
    }

    enum SortTarget {
        CREATED_AT, UPDATED_AT, TITLE, FIELD
    }

    /** {@code fieldKey} and {@code fieldType} are set only when the target is {@link SortTarget#FIELD}. */
    record Sort(SortTarget target, String fieldKey, FieldType fieldType, boolean descending) {
        static Sort newestFirst() {
            return new Sort(SortTarget.CREATED_AT, null, null, true);
        }
    }

    int offset() {
        return page * size;
    }
}
