package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCollectionRequest", description = "Update collection metadata. Fields are optional.")
public record UpdateCollectionRequest(
        @Schema(description = "New collection name", example = "Board Games (Updated)", nullable = true)
        String name,

        @Schema(description = "New description", example = "Updated description", nullable = true)
        String description
) {
}
