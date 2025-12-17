package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnableModuleResponse")
public record EnableModuleResponse(
        @Schema(example = "true") boolean enabled
) {
}
