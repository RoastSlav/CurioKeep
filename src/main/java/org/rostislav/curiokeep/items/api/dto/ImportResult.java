package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ImportResult", description = "What an import did. Items that fail are skipped; the rest are still imported.")
public record ImportResult(
        @Schema(description = "Items added to the collection.", example = "118")
        int imported,

        @Schema(description = "Items skipped because they were invalid.", example = "2")
        int failed,

        @Schema(description = "Why items were skipped, at most the first 50.")
        List<ItemError> errors
) {

    @Schema(name = "ImportItemError")
    public record ItemError(
            @Schema(description = "Position of the item in the file's items list, starting at 0.", example = "17")
            int index,

            @Schema(description = "Reason code, for example INVALID_FIELD_pages or MODULE_NOT_ENABLED.", example = "INVALID_FIELD_pages")
            String reason
    ) {
    }
}
