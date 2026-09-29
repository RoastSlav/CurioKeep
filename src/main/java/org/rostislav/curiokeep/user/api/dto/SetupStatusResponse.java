package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetupStatusResponse")
public record SetupStatusResponse(
        @Schema(example = "true") boolean setupRequired,
        @Schema(description = "True when creating the first admin needs the server's setup token", example = "false") boolean tokenRequired
) {
}