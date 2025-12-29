package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "InviteValidateResponse")
public record InviteValidateResponse(
        @Schema(description = "Whether the invite is valid and unused") boolean valid,
        @Schema(description = "Optional reason if invalid", nullable = true) String reason,
        @Schema(description = "Collection ID", nullable = true) UUID collectionId,
        @Schema(description = "Role granted", nullable = true) Role role
) {
}
