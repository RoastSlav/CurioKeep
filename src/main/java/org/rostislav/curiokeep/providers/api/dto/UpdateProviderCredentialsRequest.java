package org.rostislav.curiokeep.providers.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Payload for updating provider credentials")
public record UpdateProviderCredentialsRequest(
        @Schema(description = "Map of credential field names to their values", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Size(max = 20) Map<String, String> values
) {
}
