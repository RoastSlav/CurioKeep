package org.rostislav.curiokeep.collections.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateCollectionRequest", description = "Create a new collection.")
public record CreateCollectionRequest(
        @Schema(description = "Collection name", example = "Board Games", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String name,

        @Schema(description = "Optional description", example = "My physical board game library", nullable = true)
        @Size(max = 2000) String description
) {
}
