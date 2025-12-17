package org.rostislav.curiokeep.providers.api.dto;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.List;

public record ProviderStatusResponse(
        String key,
        boolean available,
        String message,
        List<ItemIdentifierEntity.IdType> supportedIdTypes
) {
}
