package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateMemberRoleRequest")
public record UpdateMemberRoleRequest(
        @Schema(description = "New role", example = "EDITOR") Role role
) {
}
