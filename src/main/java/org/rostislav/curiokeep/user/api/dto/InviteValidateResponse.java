package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InviteValidateResponse", description = "Invite validity result")
public record InviteValidateResponse(
        @Schema(example = "true") boolean valid
) {
}
