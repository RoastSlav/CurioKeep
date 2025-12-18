package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateCollectionInviteRequest")
public record CreateCollectionInviteRequest(
        @Schema(description = "Role to grant when accepted", example = "EDITOR") Role role,
        @Schema(description = "Invite expiry in days", example = "7") Integer expiresInDays
) {
}
