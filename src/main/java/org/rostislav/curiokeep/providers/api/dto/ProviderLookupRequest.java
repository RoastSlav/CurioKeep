package org.rostislav.curiokeep.providers.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProviderLookupRequest(
        @NotNull UUID moduleId,
        @NotNull List<ProviderLookupIdentifierDto> identifiers
) {
}