package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(
        name = "UpdateItemRequest",
        description = "Update item fields. Any field can be omitted (null) to leave it unchanged."
)
public record UpdateItemRequest(

        @Schema(
                description = "New state key (must exist in module contract). If null, state is not changed.",
                example = "WISHLIST",
                nullable = true
        )
        String stateKey,

        @Schema(
                description = "New display title. If null, title is not changed.",
                example = "The Hobbit",
                nullable = true
        )
        String title,

        @Schema(
                description = "New attributes JSON object. If provided, it is validated against the module contract.",
                example = "{\"title\":\"The Hobbit\",\"authors\":\"J.R.R. Tolkien\"}",
                nullable = true
        )
        Map<String, Object> attributes,

        @Schema(
                description = "If provided, replaces identifiers for this item (full replace, not merge).",
                nullable = true
        )
        List<ItemIdentifierDto> identifiers
) {
}