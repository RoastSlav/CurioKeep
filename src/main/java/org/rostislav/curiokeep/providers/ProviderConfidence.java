package org.rostislav.curiokeep.providers;

public record ProviderConfidence(
        int score,
        String reason
) {
}

