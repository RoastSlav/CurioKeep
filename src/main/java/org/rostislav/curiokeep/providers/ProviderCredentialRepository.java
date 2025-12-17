package org.rostislav.curiokeep.providers;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderCredentialRepository extends JpaRepository<ProviderCredentialEntity, String> {
}
