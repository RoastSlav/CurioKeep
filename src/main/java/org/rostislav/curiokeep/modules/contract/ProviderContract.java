package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record ProviderContract(
        String key,
        boolean enabled,
        int priority,
        List<IdentifierType> supportsIdentifiers,
        Map<String, Object> extensions,
        List<ProviderChain> chains
) {
    public ProviderContract {
        supportsIdentifiers = supportsIdentifiers == null ? List.of() : List.copyOf(supportsIdentifiers);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
        chains = chains == null ? List.of() : List.copyOf(chains);
    }

    /** A provider that follows no chains. */
    public ProviderContract(String key, boolean enabled, int priority, List<IdentifierType> supportsIdentifiers, Map<String, Object> extensions) {
        this(key, enabled, priority, supportsIdentifiers, extensions, List.of());
    }
}