package org.rostislav.curiokeep.collections.api.dto;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCollectionRequest", description = "Update collection metadata. Fields are optional.")
public record UpdateCollectionRequest(
        @Schema(description = "New collection name", example = "Board Games (Updated)", nullable = true)
        @Size(max = 200) String name,

        @Schema(description = "New description", example = "Updated description", nullable = true)
        @Size(max = 2000) String description
) {
}
