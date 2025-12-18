-- Collection-scoped invites

CREATE TABLE IF NOT EXISTS collection_invite (
    token               TEXT PRIMARY KEY,
    collection_id       UUID NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
    role                TEXT NOT NULL,
    created_by_user_id  UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ,
    accepted_by_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    accepted_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    CONSTRAINT chk_collection_invite_role CHECK (role IN ('ADMIN','EDITOR','VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_collection_invite_collection
    ON collection_invite(collection_id);

CREATE INDEX IF NOT EXISTS idx_collection_invite_expires
    ON collection_invite(expires_at);

CREATE INDEX IF NOT EXISTS idx_collection_invite_created_by
    ON collection_invite(created_by_user_id);
