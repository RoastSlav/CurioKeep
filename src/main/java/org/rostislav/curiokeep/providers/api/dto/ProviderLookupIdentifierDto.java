package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

@Schema(description = "Identifier to use for provider lookups")
public record ProviderLookupIdentifierDto(
        @NotNull @Schema(description = "Type of identifier (ISBN10, UPC, etc.)") ItemIdentifierEntity.IdType idType,
        @NotBlank @Schema(description = "Value of the identifier to query") String idValue
) {
}