package org.rostislav.curiokeep.user.api.dto;

import org.rostislav.curiokeep.user.ValidPassword;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateAdminRequest")
public record CreateAdminRequest(
        @Schema(example = "admin@curiokeep.local") @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "Str0ngP@ssw0rd!") @NotBlank @ValidPassword String password,
        @Schema(example = "Admin") @NotBlank @Size(max = 100) String displayName,
        @Schema(description = "Required only when the server is configured with a setup token") @Size(max = 200) String setupToken
) {
}