package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "CreateCollectionInviteResponse")
public record CreateCollectionInviteResponse(
        @Schema(description = "Invite token") String token,
        @Schema(description = "Role that will be granted") Role role,
        @Schema(description = "When the invite expires", nullable = true) OffsetDateTime expiresAt,
        @Schema(description = "Collection ID") UUID collectionId
) {
}
