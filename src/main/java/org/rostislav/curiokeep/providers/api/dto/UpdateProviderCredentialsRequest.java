package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Payload for updating provider credentials")
public record UpdateProviderCredentialsRequest(
        @Schema(description = "Map of credential field names to their values", required = true) Map<String, String> values
) {
}
