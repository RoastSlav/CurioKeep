package org.rostislav.curiokeep.user.api.dto;

import org.rostislav.curiokeep.user.ValidPassword;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcceptInviteRequest", description = "Accept an invite and create a local account")
public record AcceptInviteRequest(
        @Schema(example = "b0f2a7c2... (long token)", description = "Raw invite token received from the admin") @NotBlank @Size(max = 200) String token,
        @Schema(example = "Str0ngP@ssw0rd!", description = "Password for the new local account") @NotBlank @ValidPassword String password,
        @Schema(example = "RoastSlav", description = "Display name for the new user") @NotBlank @Size(max = 100) String displayName
) {
}