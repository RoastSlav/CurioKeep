package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credential storage status for a provider")
public record ProviderCredentialStatusResponse(
        @Schema(description = "Provider key") String key,
        @Schema(description = "True when the required credentials are stored") boolean credentialsConfigured
) {
}
