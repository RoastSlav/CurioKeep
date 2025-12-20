package org.rostislav.curiokeep.providers.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Metadata lookup request tying a module to identifier list")
public record ProviderLookupRequest(
        @NotNull @Schema(description = "Module ID whose providers should be queried") UUID moduleId,
        @NotNull @Schema(description = "Identifiers to send to the providers") List<ProviderLookupIdentifierDto> identifiers,
        @Schema(description = "Optional explicit provider keys to use (defaults to module providers)") List<String> providers,
        @Schema(description = "Optional free-text query to use when no identifiers are present") String query
) {
}