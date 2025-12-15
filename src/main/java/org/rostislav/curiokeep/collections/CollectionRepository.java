package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {
    List<CollectionEntity> findAllByOwnerUserId(UUID ownerUserId);
}
