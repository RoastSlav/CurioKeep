package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "ChangeStateRequest",
        description = "Request to change an item's workflow state."
)
public record ChangeStateRequest(
        @Schema(
                description = "Target state key as defined in the module contract (case-insensitive).",
                example = "OWNED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String stateKey
) {
}