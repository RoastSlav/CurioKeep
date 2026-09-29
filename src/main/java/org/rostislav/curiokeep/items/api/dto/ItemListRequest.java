package org.rostislav.curiokeep.items.api.dto;

import java.util.Map;
import java.util.UUID;

/**
 * The raw parameters of an item listing. {@code state} is a comma separated list and {@code sort} is {@code field[,asc|desc]}.
 * {@code otherParams} carries the remaining query parameters; those named {@code <fieldKey>.<operator>} are field filters.
 */
public record ItemListRequest(
        UUID moduleId,
        String search,
        String state,
        String sort,
        int page,
        int size,
        Map<String, String> otherParams
) {
}
