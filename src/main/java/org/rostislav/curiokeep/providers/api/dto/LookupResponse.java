package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.providers.ProviderAsset;
import org.rostislav.curiokeep.providers.ProviderResult;

import java.util.List;
import java.util.Map;

/**
 * Response of a provider lookup operation.
 *
 * This DTO is returned by `ProviderLookupService.lookup(...)` and contains the
 * individual provider results, the selected best result (highest confidence),
 * a merged attributes map assembled according to module provider priorities,
 * and the list of deduplicated provider assets.
 */
@Schema(name = "LookupResponse", description = "Provider lookup result containing merged attributes and assets")
public record LookupResponse(
        @Schema(description = "All provider results in evaluation order") List<ProviderResult> results,
        @Schema(description = "Selected best provider result (by priority/score)") ProviderResult best,
        @Schema(description = "Merged attributes populated from provider mappings") Map<String, Object> mergedAttributes,
        @Schema(description = "Deduplicated provider assets (images, files)") List<ProviderAsset> assets
) {
}
