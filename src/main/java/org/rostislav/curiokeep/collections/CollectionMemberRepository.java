package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionMemberRepository extends JpaRepository<CollectionMemberEntity, CollectionMemberId> {
    List<CollectionMemberEntity> findAllByIdUserId(UUID userId);

    List<CollectionMemberEntity> findAllByIdCollectionId(UUID collectionId);

    Optional<CollectionMemberEntity> findByIdCollectionIdAndIdUserId(UUID collectionId, UUID userId);

    void deleteAllByIdCollectionId(UUID collectionId);
}
