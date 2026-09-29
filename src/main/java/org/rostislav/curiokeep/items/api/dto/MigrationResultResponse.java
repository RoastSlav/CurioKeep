package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MigrationResultResponse", description = "What accepting a migration did.")
public record MigrationResultResponse(
        @Schema(description = "Items brought up to the module's current version.", example = "120")
        long migrated,

        @Schema(description = "Of those, how many had their attributes changed.", example = "87")
        long changed,

        @Schema(description = "Items left alone because their stored attributes could not be read.", example = "0")
        long skipped
) {
}
