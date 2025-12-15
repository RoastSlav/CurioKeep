package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OkResponse")
public record OkResponse(
        @Schema(example = "true") boolean ok
) {
}