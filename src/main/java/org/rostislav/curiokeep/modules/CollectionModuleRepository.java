package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionModuleRepository extends JpaRepository<CollectionModuleEntity, CollectionModuleId> {
    boolean existsByCollectionIdAndModuleId(UUID collectionId, UUID moduleId);
    Optional<CollectionModuleEntity> findByCollectionIdAndModuleId(UUID collectionId, UUID moduleId);
    List<CollectionModuleEntity> findAllByCollectionId(UUID collectionId);
}
