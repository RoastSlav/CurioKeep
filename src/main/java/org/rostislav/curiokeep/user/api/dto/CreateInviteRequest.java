package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateInviteRequest", description = "Request to create an invite for a new user")
public record CreateInviteRequest(
        @Schema(example = "user@curiokeep.local", description = "Email to invite") String email
) {
}
