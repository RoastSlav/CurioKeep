package org.rostislav.curiokeep.providers.api.dto;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.List;

public record ProviderInfoResponse(
        String key,
        String displayName,
        String description,
        List<ItemIdentifierEntity.IdType> supportedIdTypes,
        Integer priority
) {
}
