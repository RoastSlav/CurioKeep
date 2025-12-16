package org.rostislav.curiokeep.providers;

import java.util.List;
import java.util.Map;

public record ProviderResult(
        String providerKey,
        Map<String, Object> rawData,
        Map<String, Object> normalizedFields,
        List<ProviderAsset> assets,
        ProviderConfidence confidence
) {
}
