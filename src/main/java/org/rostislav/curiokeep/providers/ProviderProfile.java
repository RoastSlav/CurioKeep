package org.rostislav.curiokeep.providers;

import java.util.List;
import java.util.Objects;

public record ProviderProfile(
        String key,
        String displayName,
        String summary,
        String websiteUrl,
        String apiUrl,
        String dataReturned,
        List<String> highlights
) {
    public ProviderProfile {
        Objects.requireNonNull(key, "key is required");
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }
}
