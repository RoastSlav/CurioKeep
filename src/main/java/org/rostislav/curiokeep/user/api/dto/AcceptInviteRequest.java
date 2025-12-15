package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcceptInviteRequest", description = "Accept an invite and create a local account")
public record AcceptInviteRequest(
        @Schema(example = "b0f2a7c2... (long token)", description = "Raw invite token received from the admin") String token,
        @Schema(example = "Str0ngP@ssw0rd!", description = "Password for the new local account") String password,
        @Schema(example = "RoastSlav", description = "Display name for the new user") String displayName
) {}