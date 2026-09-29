package org.rostislav.curiokeep.items;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Looks up the values items hold in one attribute, for fields a module declares unique within a collection. Both queries are
 * bounded by the items of one module in one collection, which the {@code (collection_id, module_id, ...)} index narrows to.
 */
@Repository
class ItemUniquenessRepository {

    private final EntityManager entityManager;

    ItemUniquenessRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** True when another item of the module holds {@code value} in the attribute, ignoring case and surrounding spaces. */
    @Transactional(readOnly = true)
    boolean isTaken(UUID collectionId, UUID moduleId, String fieldKey, String value, UUID excludedItemId) {
        Query query = entityManager.createNativeQuery("SELECT 1 FROM item WHERE collection_id = :collectionId AND module_id = :moduleId"
                + " AND lower(btrim(jsonb_extract_path_text(attributes, :fieldKey))) = lower(btrim(:value))"
                + (excludedItemId == null ? "" : " AND id <> :excludedItemId") + " LIMIT 1");
        query.setParameter("collectionId", collectionId);
        query.setParameter("moduleId", moduleId);
        query.setParameter("fieldKey", fieldKey);
        query.setParameter("value", value);
        if (excludedItemId != null) query.setParameter("excludedItemId", excludedItemId);
        return !query.getResultList().isEmpty();
    }

    /** Every value the module's items hold in the attribute, as stored. */
    @Transactional(readOnly = true)
    List<String> valuesOf(UUID collectionId, UUID moduleId, String fieldKey) {
        Query query = entityManager.createNativeQuery("SELECT jsonb_extract_path_text(attributes, :fieldKey) FROM item"
                + " WHERE collection_id = :collectionId AND module_id = :moduleId AND jsonb_extract_path_text(attributes, :fieldKey) IS NOT NULL");
        query.setParameter("collectionId", collectionId);
        query.setParameter("moduleId", moduleId);
        query.setParameter("fieldKey", fieldKey);
        return query.getResultList().stream().map(String.class::cast).toList();
    }
}
