package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateInviteResponse", description = "Invite token returned once for sharing with the user")
public record CreateInviteResponse(
        @Schema(example = "b0f2a7c2... (long token)", description = "Raw invite token (shown only once)") String token
) {
}
