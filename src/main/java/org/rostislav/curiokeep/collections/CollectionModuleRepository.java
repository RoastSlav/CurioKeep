package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollectionModuleRepository extends JpaRepository<CollectionModuleEntity, CollectionModuleId> {

    boolean existsByIdCollectionIdAndIdModuleId(UUID collectionId, UUID moduleId);

    List<CollectionModuleEntity> findAllByIdCollectionId(UUID collectionId);

    void deleteByIdCollectionIdAndIdModuleId(UUID collectionId, UUID moduleId);
}