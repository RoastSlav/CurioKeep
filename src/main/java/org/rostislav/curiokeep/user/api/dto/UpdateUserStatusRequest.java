package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateUserStatusRequest", description = "Update a user's status")
public record UpdateUserStatusRequest(
        @Schema(example = "ACTIVE", description = "New status: ACTIVE or DISABLED") String status
) {
}
