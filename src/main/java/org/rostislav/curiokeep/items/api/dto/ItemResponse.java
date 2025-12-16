package org.rostislav.curiokeep.items.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.items.entities.ItemEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(
        name = "ItemResponse",
        description = "Item representation. Attributes are a module-defined JSON object."
)
public record ItemResponse(

        @Schema(description = "Item id.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Collection id that owns this item.", example = "7f7b3b3a-2d7b-4e05-9c91-2d57c29c7b55")
        UUID collectionId,

        @Schema(description = "Module definition id for this item.", example = "6e2deb6d-5d5d-4d15-be5a-d7cef35ac64d")
        UUID moduleId,

        @Schema(description = "Current state key as defined by the module contract.", example = "OWNED")
        String stateKey,

        @Schema(description = "Optional display title (can duplicate module fields like `title`).", example = "The Hobbit")
        String title,

        @Schema(
                description = "Module-defined attributes JSON object. Must conform to module contract fields.",
                example = "{\"title\":\"The Hobbit\",\"authors\":\"J.R.R. Tolkien\",\"isbn13\":\"9780261102217\"}"
        )
        Map<String, Object> attributes,

        @Schema(description = "User id that created the item.", example = "b1f28b3b-2d7b-4e05-9c91-2d57c29c7b55", nullable = true)
        UUID createdBy,

        @Schema(description = "Creation timestamp (UTC).", example = "2025-12-15T17:18:02.382311Z")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp (UTC).", example = "2025-12-15T17:18:02.382311Z")
        OffsetDateTime updatedAt
) {
    public static ItemResponse from(ItemEntity e, ObjectMapper mapper) {
        return new ItemResponse(
                e.getId(),
                e.getCollectionId(),
                e.getModuleId(),
                e.getStateKey(),
                e.getTitle(),
                mapper.convertValue(e.getAttributes(), Map.class),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}