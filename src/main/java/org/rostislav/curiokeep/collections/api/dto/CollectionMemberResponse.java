package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "CollectionMemberResponse", description = "Member of a collection")
public record CollectionMemberResponse(
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Email") String email,
        @Schema(description = "Display name") String displayName,
        @Schema(description = "Role in the collection") Role role,
        @Schema(description = "When the member joined") OffsetDateTime joinedAt
) {
}
