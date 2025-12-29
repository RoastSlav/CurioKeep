package org.rostislav.curiokeep.providers;

import java.util.Optional;

public interface ProviderCredentialLookup {
    Optional<ProviderCredential> getCredentials(String providerKey);
}
