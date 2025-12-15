package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateAdminRequest")
public record CreateAdminRequest(
        @Schema(example = "admin@curiokeep.local") String email,
        @Schema(example = "Str0ngP@ssw0rd!") String password,
        @Schema(example = "Admin") String displayName
) {}