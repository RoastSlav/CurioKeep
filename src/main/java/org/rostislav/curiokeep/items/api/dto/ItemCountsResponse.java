package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(name = "ItemCountsResponse", description = "How many items a collection holds, per module and per state.")
public record ItemCountsResponse(
        @Schema(description = "Counts keyed by module id. A module with no items is absent.")
        Map<UUID, ModuleCounts> modules
) {

    @Schema(name = "ModuleCounts")
    public record ModuleCounts(
            @Schema(description = "All items of the module in the collection.", example = "120")
            long total,

            @Schema(description = "Items per state key. A state with no items is absent.", example = "{\"OWNED\": 100, \"WISHLIST\": 20}")
            Map<String, Long> byState,

            @Schema(description = "For each deprecated field of the module that still has values, how many items hold one.", example = "{\"authors_text\": 42}")
            Map<String, Long> deprecatedFieldUse
    ) {
    }
}
