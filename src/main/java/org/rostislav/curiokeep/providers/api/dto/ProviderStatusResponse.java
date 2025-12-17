package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.List;

@Schema(description = "Readiness status for an individual provider")
public record ProviderStatusResponse(
        @Schema(description = "Provider key") String key,
        @Schema(description = "True when the provider is configured and responsive") boolean available,
        @Schema(description = "Optional status detail (errors or confirmations)") String message,
        @Schema(description = "Identifier types that the provider still accepts") List<ItemIdentifierEntity.IdType> supportedIdTypes
) {
}
