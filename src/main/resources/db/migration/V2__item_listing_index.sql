-- V2__item_listing_index.sql

-- serves ItemSearchRepository: the default listing (newest first within a collection's module) and the stable tie-breaker
-- (created_at, id) that every sort ends with; its leading columns also cover the collection ON DELETE CASCADE
CREATE INDEX idx_item_collection_module_created ON item (collection_id, module_id, created_at DESC, id);

-- now redundant: same leading columns as the index above
DROP INDEX idx_item_collection_module;
