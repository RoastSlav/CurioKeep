package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.modules.contract.ModuleSource;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "CollectionModuleResponse")
public record CollectionModuleResponse(
        @Schema(example = "books") String moduleKey,
        @Schema(example = "Books") String name,
        @Schema(example = "1.0.0") String version,
        @Schema(example = "BUILTIN") ModuleSource source,
        @Schema(example = "2025-12-16T12:00:00+02:00") OffsetDateTime enabledAt,
        UUID moduleId
) {
}
