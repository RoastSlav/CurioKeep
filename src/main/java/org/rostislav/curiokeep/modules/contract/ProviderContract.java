package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record ProviderContract(
        String key,
        boolean enabled,
        int priority,
        List<IdentifierType> supportsIdentifiers,
        Map<String, Object> extensions
) {
    public ProviderContract {
        supportsIdentifiers = supportsIdentifiers == null ? List.of() : List.copyOf(supportsIdentifiers);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}