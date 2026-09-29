package org.rostislav.curiokeep.user.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateUserStatusRequest", description = "Update a user's status")
public record UpdateUserStatusRequest(
        @Schema(example = "ACTIVE", description = "New status: ACTIVE or DISABLED") @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status
) {
}
