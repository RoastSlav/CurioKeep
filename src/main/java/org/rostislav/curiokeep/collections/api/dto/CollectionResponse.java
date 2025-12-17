package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "CollectionResponse", description = "A collection visible to the current user.")
public record CollectionResponse(
        @Schema(description = "Collection ID", example = "b91b4c5f-1a4d-4c4f-8b39-3f8a9e6d2e22")
        UUID id,

        @Schema(description = "Collection name", example = "Board Games")
        String name,

        @Schema(description = "Optional description", example = "My physical board game library", nullable = true)
        String description,

        @Schema(description = "Current user's role in this collection", example = "OWNER")
        String role,

        @Schema(description = "When the collection was created (server time)", example = "2025-12-15T22:10:00+02:00")
        OffsetDateTime createdAt
) {
    public static CollectionResponse from(CollectionEntity c, Role role) {
        return new CollectionResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                role.name(),
                c.getCreatedAt()
        );
    }
}
