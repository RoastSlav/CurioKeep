package org.rostislav.curiokeep.collections.invites;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionInviteRepository extends JpaRepository<CollectionInviteEntity, String> {
    Optional<CollectionInviteEntity> findByToken(String token);
    List<CollectionInviteEntity> findAllByCollectionIdAndAcceptedAtIsNullAndRevokedAtIsNull(UUID collectionId);
}
