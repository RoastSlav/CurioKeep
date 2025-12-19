package org.rostislav.curiokeep.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ScanModulesResponse", description = "Summary of the import folder scan operation")
public record ScanModulesResponse(
        List<ModuleSummaryResponse> imported,
        List<String> skipped,
        List<ScanFailure> failed
) {
    @Schema(name = "ScanFailure", description = "Represents one file that failed to import")
    public record ScanFailure(
            String source,
            String reason
    ) {
    }
}
