package org.rostislav.curiokeep.providers;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderRegistry {

    private final Map<String, MetadataProvider> providersByKey;

    public ProviderRegistry(List<MetadataProvider> providers) {
        Map<String, MetadataProvider> map = new HashMap<>();
        for (MetadataProvider p : providers) {
            map.put(p.key(), p);
        }
        this.providersByKey = Collections.unmodifiableMap(map);
    }

    public Optional<MetadataProvider> get(String key) {
        return Optional.ofNullable(providersByKey.get(key));
    }

    public Collection<MetadataProvider> all() {
        return providersByKey.values();
    }
}
