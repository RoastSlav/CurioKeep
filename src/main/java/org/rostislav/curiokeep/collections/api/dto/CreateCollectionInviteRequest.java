package org.rostislav.curiokeep.collections.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateCollectionInviteRequest")
public record CreateCollectionInviteRequest(
        @Schema(description = "Role to grant when accepted", example = "EDITOR") @NotNull Role role,
        @Schema(description = "Invite expiry in days", example = "7") @Min(1) @Max(365) Integer expiresInDays
) {
}
