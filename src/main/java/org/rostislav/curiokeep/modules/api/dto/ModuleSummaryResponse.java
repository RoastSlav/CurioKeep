package org.rostislav.curiokeep.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "ModuleSummaryResponse", description = "Summary information about a module definition.")
public record ModuleSummaryResponse(
        UUID id,
        @Schema(example = "books") String moduleKey,
        @Schema(example = "Books") String name,
        @Schema(example = "1.0.0") String version,
        @Schema(example = "BUILTIN") ModuleSource source,
        OffsetDateTime updatedAt
) {
    public static ModuleSummaryResponse from(ModuleDefinitionEntity m) {
        return new ModuleSummaryResponse(
                m.getId(),
                m.getModuleKey(),
                m.getName(),
                m.getVersion(),
                m.getSource(),
                m.getUpdatedAt()
        );
    }
}