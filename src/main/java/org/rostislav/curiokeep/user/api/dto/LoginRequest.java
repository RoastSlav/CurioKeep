package org.rostislav.curiokeep.user.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(example = "admin@curiokeep.local") @NotBlank @Size(max = 254) String email,
        @Schema(example = "Str0ngP@ssw0rd!") @NotBlank @Size(max = 256) String password
) {
}
