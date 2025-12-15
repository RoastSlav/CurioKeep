package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemIdentifierRepository extends JpaRepository<ItemIdentifierEntity, UUID> {
    List<ItemIdentifierEntity> findAllByItemId(UUID itemId);
    Optional<ItemIdentifierEntity> findByIdTypeAndIdValue(String idType, String idValue);
}
