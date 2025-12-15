package org.rostislav.curiokeep.user;

import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {
    boolean existsByEmail(String email);
    Optional<AppUserEntity> findByEmailIgnoreCase(String email);
    boolean existsByIsAdminTrue();
}
