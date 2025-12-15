package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    Page<ItemEntity> findAllByCollectionIdAndModuleId(UUID collectionId, UUID moduleId, Pageable pageable);
}
