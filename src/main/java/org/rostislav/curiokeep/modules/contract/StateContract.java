package org.rostislav.curiokeep.modules.contract;

import java.util.Map;

public record StateContract(
        String key,
        String label,
        int order,
        boolean active,
        boolean deprecated,
        Map<String, Object> extensions
) {
    public StateContract {
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}