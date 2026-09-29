package org.rostislav.curiokeep.items;

import java.time.OffsetDateTime;
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

    /** First batch of a module's items in creation order; continue with {@link #findBatchAfter}. */
    @Query("select i from ItemEntity i where i.collectionId = :collectionId and i.moduleId = :moduleId "
            + "order by i.createdAt asc, i.id asc")
    List<ItemEntity> findFirstBatch(@Param("collectionId") UUID collectionId, @Param("moduleId") UUID moduleId, Pageable batch);

    /** The batch after the item with the given creation time and id, so a long export never uses OFFSET. */
    @Query("select i from ItemEntity i where i.collectionId = :collectionId and i.moduleId = :moduleId "
            + "and (i.createdAt > :createdAt or (i.createdAt = :createdAt and i.id > :id)) "
            + "order by i.createdAt asc, i.id asc")
    List<ItemEntity> findBatchAfter(@Param("collectionId") UUID collectionId, @Param("moduleId") UUID moduleId,
                                    @Param("createdAt") OffsetDateTime createdAt, @Param("id") UUID id, Pageable batch);

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
