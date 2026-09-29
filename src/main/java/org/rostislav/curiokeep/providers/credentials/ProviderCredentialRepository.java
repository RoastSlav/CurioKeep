package org.rostislav.curiokeep.providers.credentials;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderCredentialRepository extends JpaRepository<ProviderCredentialEntity, String> {
}
