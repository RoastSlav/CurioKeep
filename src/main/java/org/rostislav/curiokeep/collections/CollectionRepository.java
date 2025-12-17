package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {
    List<CollectionEntity> findAllByIdIn(Collection<UUID> ids);

    Optional<CollectionEntity> findById(UUID id);
}
