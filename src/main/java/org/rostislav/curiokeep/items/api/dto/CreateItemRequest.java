package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateItemRequest(

        @Schema(description = "Module id for this item")
        @NotNull UUID moduleId,

        @Schema(description = "Initial state key", example = "OWNED")
        String stateKey,

        @Schema(description = "Optional display title")
        String title,

        @Schema(
                description = "Arbitrary module attributes JSON",
                type = "object",
                example = """
                        {
                          "isbn13": "9780306406157",
                          "authors": ["Frank Herbert"],
                          "pages": 412
                        }
                        """
        )
        @NotNull Map<String, Object> attributes,

        @Schema(description = "Optional identifiers (ISBN etc.)")
        List<ItemIdentifierDto> identifiers
) {
}
