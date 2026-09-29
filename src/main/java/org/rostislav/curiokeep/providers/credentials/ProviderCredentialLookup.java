package org.rostislav.curiokeep.providers.credentials;

import java.util.Optional;

public interface ProviderCredentialLookup {
    Optional<ProviderCredential> getCredentials(String providerKey);
}
