package org.rostislav.curiokeep.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error response")
public record ApiError(
        @Schema(example = "SETUP_REQUIRED") String error,
        @Schema(example = "Human readable message") String message
) {}
