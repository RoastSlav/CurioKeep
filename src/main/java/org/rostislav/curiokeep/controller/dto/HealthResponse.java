package org.rostislav.curiokeep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HealthResponse", description = "Basic health check response")
public record HealthResponse(
        @Schema(example = "ok", description = "Service status") String status
) {}