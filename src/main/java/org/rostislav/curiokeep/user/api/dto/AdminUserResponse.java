package org.rostislav.curiokeep.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "AdminUserResponse", description = "User details for admin screens")
public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        boolean admin,
        String status,
        String authProvider,
        String providerSubject,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
