package org.rostislav.curiokeep.user;

import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.rostislav.curiokeep.user.entities.UserInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserInviteRepository extends JpaRepository<UserInviteEntity, UUID> {
    boolean existsByEmailIgnoreCaseAndStatus(String email, String status);

    Optional<UserInviteEntity> findByTokenHashAndStatus(String tokenHash, String status);

    long countByInvitedBy(AppUserEntity invitedBy);
}
