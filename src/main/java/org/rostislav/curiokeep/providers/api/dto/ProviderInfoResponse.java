package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.List;

@Schema(description = "Metadata about a configured provider")
public record ProviderInfoResponse(
        @Schema(description = "Internal provider key") String key,
        @Schema(description = "Human-readable provider name") String displayName,
        @Schema(description = "Brief summary of what the provider offers") String description,
        @Schema(description = "Identifier types this provider accepts") List<ItemIdentifierEntity.IdType> supportedIdTypes,
        @Schema(description = "Priority used by modules when merging results") Integer priority,
        @Schema(description = "Primary provider website") String websiteUrl,
        @Schema(description = "API documentation or base endpoint") String apiUrl,
        @Schema(description = "Short description of the data returned") String dataReturned,
        @Schema(description = "Highlight bullets such as notable fields") List<String> highlights
) {
}
