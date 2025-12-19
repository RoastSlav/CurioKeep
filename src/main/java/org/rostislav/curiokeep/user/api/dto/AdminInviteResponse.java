package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "AdminInviteResponse", description = "Invite listing for admin UI")
public record AdminInviteResponse(
        @Schema(description = "Invite identifier (UUID string)") String token,
        @Schema(description = "Email of the invited user") String email,
        @Schema(description = "Who created the invite") InvitedBy invitedBy,
        @Schema(description = "Creation timestamp") OffsetDateTime createdAt
) {
    @Schema(name = "InvitedBy", description = "Inviter information")
    public record InvitedBy(String displayName, String email) {
    }
}
