-- V1__init.sql
-- Baseline schema. Requires PostgreSQL 13 or newer (gen_random_uuid() is built in).

-- ----------------------------
-- USERS
-- ----------------------------
CREATE TABLE app_user (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email            TEXT NOT NULL UNIQUE,
    display_name     TEXT NOT NULL,
    password_hash    TEXT,
    is_admin         BOOLEAN NOT NULL DEFAULT FALSE,
    status           TEXT NOT NULL DEFAULT 'ACTIVE',
    auth_provider    TEXT NOT NULL DEFAULT 'LOCAL',
    provider_subject TEXT,
    last_login_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_app_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    CONSTRAINT chk_app_user_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'GITHUB'))
);

-- serves AppUserRepository.existsByIsAdminTrue (asked on every request by SetupModeFilter)
CREATE INDEX idx_app_user_admin ON app_user (id) WHERE is_admin;

-- One account per external identity; LOCAL users have no provider_subject
CREATE UNIQUE INDEX ux_app_user_provider_subject
    ON app_user (auth_provider, provider_subject)
    WHERE provider_subject IS NOT NULL;

-- Admin invites (invite-only signup)
CREATE TABLE user_invite (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       TEXT NOT NULL,
    token_hash  TEXT NOT NULL, -- sha256 of the token; the raw token is never stored
    invited_by  UUID NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    status      TEXT NOT NULL DEFAULT 'PENDING',
    expires_at  TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_invite_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

-- serves UserInviteRepository.findByTokenHashAndStatus
CREATE UNIQUE INDEX ux_user_invite_token_hash ON user_invite (token_hash);

-- serves UserInviteRepository.existsByEmailIgnoreCaseAndStatus; also allows one pending invite per email
CREATE UNIQUE INDEX ux_user_invite_pending_email ON user_invite (email) WHERE status = 'PENDING';

-- serves UserInviteRepository.countByInvitedBy and the ON DELETE RESTRICT check
CREATE INDEX idx_user_invite_invited_by ON user_invite (invited_by);

-- ----------------------------
-- COLLECTIONS
-- ----------------------------
CREATE TABLE collection (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    name          TEXT NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_collection_owner_user ON collection (owner_user_id);

CREATE TABLE collection_member (
    collection_id UUID NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role          TEXT NOT NULL DEFAULT 'OWNER',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, user_id),
    CONSTRAINT chk_collection_member_role CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'VIEWER'))
);

-- serves CollectionMemberRepository.findAllByIdUserId
CREATE INDEX idx_collection_member_user ON collection_member (user_id);

-- Collection-scoped invites
CREATE TABLE collection_invite (
    token               TEXT PRIMARY KEY,
    collection_id       UUID NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
    role                TEXT NOT NULL,
    created_by_user_id  UUID NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ,
    accepted_by_user_id UUID REFERENCES app_user (id) ON DELETE SET NULL,
    accepted_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    CONSTRAINT chk_collection_invite_role CHECK (role IN ('ADMIN', 'EDITOR', 'VIEWER'))
);

-- serves CollectionInviteRepository.findAllByCollectionIdAndAcceptedAtIsNullAndRevokedAtIsNull
CREATE INDEX idx_collection_invite_collection ON collection_invite (collection_id);

-- foreign-key checks on app_user deletes
CREATE INDEX idx_collection_invite_created_by ON collection_invite (created_by_user_id);

-- ----------------------------
-- MODULE DEFINITIONS (loaded from XML)
-- ----------------------------
CREATE TABLE module_definition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_key      TEXT NOT NULL UNIQUE,                 -- e.g. "books"
    name            TEXT NOT NULL,
    version         TEXT NOT NULL,                        -- semver string
    source          TEXT NOT NULL,
    checksum        TEXT NOT NULL,                        -- sha256(xml_raw)
    xml_raw         TEXT NOT NULL,
    definition_json JSONB NOT NULL DEFAULT '{}'::jsonb,   -- compiled contract
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_module_source CHECK (source IN ('BUILTIN', 'IMPORTED'))
);

CREATE TABLE module_state (
    module_id  UUID NOT NULL REFERENCES module_definition (id) ON DELETE CASCADE,
    state_key  TEXT NOT NULL,                             -- e.g. OWNED
    label      TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (module_id, state_key)
);

CREATE TABLE module_field (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id         UUID NOT NULL REFERENCES module_definition (id) ON DELETE CASCADE,
    field_key         TEXT NOT NULL,                      -- e.g. title, isbn13
    label             TEXT NOT NULL,
    field_type        TEXT NOT NULL,
    required          BOOLEAN NOT NULL DEFAULT FALSE,
    searchable        BOOLEAN NOT NULL DEFAULT FALSE,
    filterable        BOOLEAN NOT NULL DEFAULT FALSE,
    sortable          BOOLEAN NOT NULL DEFAULT FALSE,
    default_value     JSONB,
    enum_values       JSONB,
    provider_mappings JSONB,
    sort_order        INT NOT NULL DEFAULT 0,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    deprecated        BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (module_id, field_key),
    CONSTRAINT chk_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'ENUM', 'TAGS', 'LINK', 'JSON'))
);

-- Module enabled per collection
CREATE TABLE collection_module (
    collection_id UUID NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
    module_id     UUID NOT NULL REFERENCES module_definition (id) ON DELETE RESTRICT,
    enabled_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, module_id)
);

-- serves CollectionModuleRepository.existsByIdModuleId
CREATE INDEX idx_collection_module_module ON collection_module (module_id);

-- ----------------------------
-- ITEMS
-- ----------------------------
CREATE TABLE item (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id UUID NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
    module_id     UUID NOT NULL REFERENCES module_definition (id) ON DELETE RESTRICT,
    state_key     TEXT NOT NULL DEFAULT 'OWNED',
    title         TEXT,                                   -- denormalized for list views
    attributes    JSONB NOT NULL DEFAULT '{}'::jsonb,     -- dynamic fields live here
    image_name    TEXT,                                   -- locally stored cover file
    created_by    UUID REFERENCES app_user (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- a state must be one the item's module declares
    CONSTRAINT fk_item_state_per_module FOREIGN KEY (module_id, state_key)
        REFERENCES module_state (module_id, state_key) ON DELETE RESTRICT
);

-- serves ItemRepository.findAllByCollectionIdAndModuleId and the collection ON DELETE CASCADE
CREATE INDEX idx_item_collection_module ON item (collection_id, module_id);

-- serves ItemRepository.existsByModuleId, the module ON DELETE RESTRICT check, and the
-- state-usage count in ModuleLoadTx (module_id + state_key); also backs fk_item_state_per_module
CREATE INDEX idx_item_module_state ON item (module_id, state_key);

CREATE INDEX idx_item_title ON item (title);

-- JSONB search/filter support
CREATE INDEX gin_item_attributes ON item USING GIN (attributes);

-- Optional identifiers (ISBN/UPC/etc)
CREATE TABLE item_identifier (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id    UUID NOT NULL REFERENCES item (id) ON DELETE CASCADE,
    id_type    TEXT NOT NULL,
    id_value   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (item_id, id_type),
    CONSTRAINT chk_id_type CHECK (id_type IN ('ISBN10', 'ISBN13', 'UPC', 'EAN', 'ASIN', 'CUSTOM'))
);

-- serves ItemIdentifierRepository.findByIdTypeAndIdValue
CREATE INDEX idx_item_identifier_lookup ON item_identifier (id_type, id_value);

-- ----------------------------
-- PROVIDER CREDENTIALS
-- ----------------------------
CREATE TABLE provider_credentials (
    provider_key      TEXT PRIMARY KEY,
    encrypted_payload TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------
-- TRIGGERS
-- ----------------------------
-- Workaround for Hibernate: ModuleDefinitionEntity maps its states and fields as a unidirectional
-- @OneToMany, so removing a module first issues "UPDATE ... SET module_id = NULL". module_id is NOT NULL,
-- so these triggers turn that update into a delete of the child row. Removing the workaround needs a
-- bidirectional mapping (or cascade delete only) in the entity.
CREATE FUNCTION trg_delete_module_field_when_module_id_null()
    RETURNS trigger AS
$$
BEGIN
    IF NEW.module_id IS NULL THEN
        DELETE FROM module_field WHERE id = OLD.id;
        RETURN NULL; -- skip the UPDATE
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER module_field_no_null_module_id
    BEFORE UPDATE OF module_id
    ON module_field
    FOR EACH ROW
    WHEN (NEW.module_id IS NULL)
EXECUTE FUNCTION trg_delete_module_field_when_module_id_null();

CREATE FUNCTION trg_delete_module_state_when_module_id_null()
    RETURNS trigger AS
$$
BEGIN
    IF NEW.module_id IS NULL THEN
        DELETE FROM module_state
        WHERE module_id = OLD.module_id
          AND state_key = OLD.state_key;
        RETURN NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER module_state_no_null_module_id
    BEFORE UPDATE OF module_id
    ON module_state
    FOR EACH ROW
    WHEN (NEW.module_id IS NULL)
EXECUTE FUNCTION trg_delete_module_state_when_module_id_null();
