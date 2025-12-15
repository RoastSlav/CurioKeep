package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "MeResponse")
public record MeResponse(
        UUID id,
        @Schema(example = "user@curiokeep.local") String email,
        @Schema(example = "RoastSlav") String displayName,
        @Schema(example = "false") boolean isAdmin
) {
}