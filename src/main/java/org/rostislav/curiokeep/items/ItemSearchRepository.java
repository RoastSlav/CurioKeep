package org.rostislav.curiokeep.items;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Runs the item listing query, which filters and sorts on JSONB attributes and so is written as native SQL. */
@Repository
class ItemSearchRepository {

    private final EntityManager entityManager;

    ItemSearchRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    Page<ItemEntity> search(ItemQuery query) {
        ItemSearchSql sql = new ItemSearchSql(query);

        Query select = entityManager.createNativeQuery(
                "SELECT i.* FROM item i WHERE " + sql.where() + " ORDER BY " + sql.orderBy(), ItemEntity.class);
        bind(select, sql.whereParams());
        bind(select, sql.orderParams());
        select.setFirstResult(query.offset());
        select.setMaxResults(query.size());
        List<ItemEntity> rows = select.getResultList().stream().map(ItemEntity.class::cast).toList();

        Query count = entityManager.createNativeQuery("SELECT count(*) FROM item i WHERE " + sql.where());
        bind(count, sql.whereParams());
        long total = ((Number) count.getSingleResult()).longValue();

        return new PageImpl<>(rows, PageRequest.of(query.page(), query.size()), total);
    }

    /** How many items match the filters, without fetching any. */
    @Transactional(readOnly = true)
    long count(ItemQuery query) {
        ItemSearchSql sql = new ItemSearchSql(query);
        Query count = entityManager.createNativeQuery("SELECT count(*) FROM item i WHERE " + sql.where());
        bind(count, sql.whereParams());
        return ((Number) count.getSingleResult()).longValue();
    }

    private static void bind(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }
}
