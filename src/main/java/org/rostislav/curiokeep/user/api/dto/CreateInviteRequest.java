package org.rostislav.curiokeep.user.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateInviteRequest", description = "Request to create an invite for a new user")
public record CreateInviteRequest(
        @Schema(example = "user@curiokeep.local", description = "Email to invite") @NotBlank @Email @Size(max = 254) String email
) {
}
