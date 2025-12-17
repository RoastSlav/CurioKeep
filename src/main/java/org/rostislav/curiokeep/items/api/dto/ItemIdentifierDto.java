package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

@Schema(
        name = "ItemIdentifier",
        description = "External identifier for an item (ISBN/UPC/EAN/etc). One identifier per type per item."
)
public record ItemIdentifierDto(
        @Schema(
                description = "Identifier type.",
                example = "ISBN13",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull ItemIdentifierEntity.IdType idType,

        @Schema(
                description = "Identifier value. Trimmed and stored as-is (format validation can be added later).",
                example = "9780306406157",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String idValue
) {
}