package org.rostislav.curiokeep.providers.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

public record ProviderLookupIdentifierDto(
        @NotNull ItemIdentifierEntity.IdType idType,
        @NotBlank String idValue
) {
}