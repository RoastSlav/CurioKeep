-- V1__init.sql

-- UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------
-- USERS
-- ----------------------------
CREATE TABLE IF NOT EXISTS app_user (
                                        id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        email         TEXT UNIQUE,
                                        display_name  TEXT NOT NULL,
                                        password_hash TEXT,
                                        created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------
-- COLLECTIONS
-- ----------------------------
CREATE TABLE IF NOT EXISTS collection (
                                          id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          owner_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
                                          name          TEXT NOT NULL,
                                          description   TEXT,
                                          created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                          updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_collection_owner_user
    ON collection(owner_user_id);

-- simple membership model (invites later)
CREATE TABLE IF NOT EXISTS collection_member (
                                                 collection_id UUID NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
                                                 user_id       UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                                 role          TEXT NOT NULL DEFAULT 'OWNER',
                                                 created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                 PRIMARY KEY (collection_id, user_id),
                                                 CONSTRAINT chk_collection_member_role CHECK (role IN ('OWNER','ADMIN','EDITOR','VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_collection_member_user
    ON collection_member(user_id);

-- ----------------------------
-- MODULE DEFINITIONS (loaded from XML)
-- ----------------------------
CREATE TABLE IF NOT EXISTS module_definition (
                                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                 module_key      TEXT NOT NULL UNIQUE,                        -- e.g. "books"
                                                 name            TEXT NOT NULL,                               -- display name
                                                 version         TEXT NOT NULL,                               -- semver string
                                                 source          TEXT NOT NULL,                               -- BUILTIN / USER
                                                 checksum        TEXT NOT NULL,                               -- sha256(xml_raw)
                                                 xml_raw         TEXT NOT NULL,
                                                 definition_json JSONB NOT NULL DEFAULT '{}'::jsonb,          -- normalized/compiled optional
                                                 created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                 updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                 CONSTRAINT chk_module_source CHECK (source IN ('BUILTIN','USER'))
);

CREATE INDEX IF NOT EXISTS idx_module_definition_key
    ON module_definition(module_key);

-- States per module (used for validation + UI)
CREATE TABLE IF NOT EXISTS module_state (
                                            module_id   UUID NOT NULL REFERENCES module_definition(id) ON DELETE CASCADE,
                                            state_key   TEXT NOT NULL,            -- e.g. OWNED
                                            label       TEXT NOT NULL,
                                            sort_order  INT  NOT NULL DEFAULT 0,
                                            PRIMARY KEY (module_id, state_key)
);

CREATE INDEX IF NOT EXISTS idx_module_state_module
    ON module_state(module_id);

-- Fields per module
CREATE TABLE IF NOT EXISTS module_field (
                                            id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            module_id         UUID NOT NULL REFERENCES module_definition(id) ON DELETE CASCADE,
                                            field_key         TEXT NOT NULL, -- e.g. title, isbn13
                                            label             TEXT NOT NULL,
                                            field_type        TEXT NOT NULL, -- TEXT, NUMBER, DATE, BOOLEAN, ENUM, etc.
                                            required          BOOLEAN NOT NULL DEFAULT FALSE,
                                            searchable        BOOLEAN NOT NULL DEFAULT FALSE,
                                            filterable        BOOLEAN NOT NULL DEFAULT FALSE,
                                            sortable          BOOLEAN NOT NULL DEFAULT FALSE,
                                            default_value     JSONB,
                                            enum_values       JSONB,
                                            provider_mappings JSONB,
                                            sort_order        INT NOT NULL DEFAULT 0,
                                            UNIQUE (module_id, field_key),
                                            CONSTRAINT chk_field_type CHECK (field_type IN (
                                                                                            'TEXT','NUMBER','DATE','BOOLEAN','ENUM','TAGS','LINK','JSON'
                                                ))
);

CREATE INDEX IF NOT EXISTS idx_module_field_module
    ON module_field(module_id);

-- Module enabled/disabled per collection
CREATE TABLE IF NOT EXISTS collection_module (
                                                 collection_id UUID NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
                                                 module_id     UUID NOT NULL REFERENCES module_definition(id) ON DELETE RESTRICT,
                                                 enabled_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                 PRIMARY KEY (collection_id, module_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_module_module
    ON collection_module(module_id);

-- ----------------------------
-- ITEMS
-- ----------------------------
CREATE TABLE IF NOT EXISTS item (
                                    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    collection_id UUID NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
                                    module_id     UUID NOT NULL REFERENCES module_definition(id) ON DELETE RESTRICT,
                                    state_key     TEXT NOT NULL DEFAULT 'OWNED',
                                    title         TEXT,                                   -- optional denormalized for list views
                                    attributes    JSONB NOT NULL DEFAULT '{}'::jsonb,      -- dynamic fields live here
                                    created_by    UUID REFERENCES app_user(id) ON DELETE SET NULL,
                                    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_item_collection
    ON item(collection_id);

CREATE INDEX IF NOT EXISTS idx_item_module
    ON item(module_id);

CREATE INDEX IF NOT EXISTS idx_item_collection_module
    ON item(collection_id, module_id);

CREATE INDEX IF NOT EXISTS idx_item_state
    ON item(state_key);

CREATE INDEX IF NOT EXISTS idx_item_title
    ON item(title);

-- JSONB search/filter support
CREATE INDEX IF NOT EXISTS gin_item_attributes
    ON item USING GIN (attributes);

-- Enforce valid state per module
ALTER TABLE item
    ADD CONSTRAINT fk_item_state_per_module
        FOREIGN KEY (module_id, state_key)
            REFERENCES module_state(module_id, state_key)
            ON DELETE RESTRICT;

-- Optional identifiers (ISBN/UPC/etc)
CREATE TABLE IF NOT EXISTS item_identifier (
                                               id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               item_id     UUID NOT NULL REFERENCES item(id) ON DELETE CASCADE,
                                               id_type     TEXT NOT NULL,   -- ISBN10, ISBN13, UPC, EAN, etc
                                               id_value    TEXT NOT NULL,
                                               created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                               UNIQUE (item_id, id_type),
                                               CONSTRAINT chk_id_type CHECK (id_type IN ('ISBN10','ISBN13','UPC','EAN','ASIN','CUSTOM'))
);

-- Fast lookups by identifier value:
CREATE INDEX IF NOT EXISTS idx_item_identifier_lookup
    ON item_identifier(id_type, id_value);