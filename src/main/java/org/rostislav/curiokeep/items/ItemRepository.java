package org.rostislav.curiokeep.items;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    Page<ItemEntity> findAllByCollectionIdAndModuleId(UUID collectionId, UUID moduleId, Pageable pageable);

    boolean existsByModuleId(UUID moduleId);

    boolean existsByImageName(String imageName);

    /** One row per module and state that has items, which is far less than one per item. */
    @Query("select i.moduleId as moduleId, i.stateKey as stateKey, count(i) as count "
            + "from ItemEntity i where i.collectionId = :collectionId group by i.moduleId, i.stateKey")
    List<StateCount> countByModuleAndState(@Param("collectionId") UUID collectionId);

    interface StateCount {
        UUID getModuleId();

        String getStateKey();

        long getCount();
    }
}
