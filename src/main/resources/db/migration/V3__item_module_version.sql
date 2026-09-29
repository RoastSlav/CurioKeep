-- The module version an item's attributes were last brought up to. A module update leaves items on the version they were saved
-- under until a collection admin accepts the migration the module declares, so the app can tell which items are behind.
ALTER TABLE item ADD COLUMN module_version TEXT;

-- Every existing item is treated as current: nothing was ever migrated before this column existed.
UPDATE item SET module_version = module_definition.version FROM module_definition WHERE module_definition.id = item.module_id;

ALTER TABLE item ALTER COLUMN module_version SET NOT NULL;
