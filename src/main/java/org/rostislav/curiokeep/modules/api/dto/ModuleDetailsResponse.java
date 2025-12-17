package org.rostislav.curiokeep.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.ModuleSource;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "ModuleDetailsResponse", description = "Full module definition details including compiled contract JSON.")
public record ModuleDetailsResponse(
        UUID id,
        @Schema(example = "books") String moduleKey,
        @Schema(example = "Books") String name,
        @Schema(example = "1.0.0") String version,
        @Schema(example = "BUILTIN") ModuleSource source,
        @Schema(example = "c0ffee...") String checksum,
        ModuleContract contract,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
