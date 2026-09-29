package org.rostislav.curiokeep.providers.credentials;

import java.time.Instant;
import java.util.Map;

public record ProviderCredential(
        String providerKey,
        Map<String, String> values,
        Instant updatedAt
) {
}
