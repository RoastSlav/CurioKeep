package org.rostislav.curiokeep.providers;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestProviderCredentialLookup implements ProviderCredentialLookup {

    private final ConcurrentMap<String, ProviderCredential> store = new ConcurrentHashMap<>();

    public TestProviderCredentialLookup with(String providerKey, Map<String, String> values) {
        store.put(providerKey, new ProviderCredential(providerKey, Map.copyOf(values), Instant.now()));
        return this;
    }

    @Override
    public Optional<ProviderCredential> getCredentials(String providerKey) {
        return Optional.ofNullable(store.get(providerKey));
    }
}
