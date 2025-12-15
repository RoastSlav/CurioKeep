package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectionMemberRepository extends JpaRepository<CollectionMemberEntity, CollectionMemberId> {
    Optional<CollectionMemberEntity> findByCollectionIdAndUserId(UUID collectionId, UUID userId);

    boolean existsByCollectionIdAndUserId(UUID collectionId, UUID userId);
}
