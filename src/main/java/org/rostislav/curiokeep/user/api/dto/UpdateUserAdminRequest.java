package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateUserAdminRequest", description = "Grant or revoke admin rights")
public record UpdateUserAdminRequest(
        @Schema(example = "true") boolean admin
) {
}
