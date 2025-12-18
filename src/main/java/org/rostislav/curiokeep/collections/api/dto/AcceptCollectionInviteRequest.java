package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcceptCollectionInviteRequest")
public record AcceptCollectionInviteRequest(
        @Schema(description = "Invite token") String token
) {
}
