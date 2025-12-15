-- V2__users_admin_status_invites.sql

-- ----------------------------
-- USERS: admin + status + OAuth-ready fields
-- ----------------------------

-- 1) Add new columns (safe defaults)
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS is_admin        BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status          TEXT        NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS auth_provider   TEXT        NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS provider_subject TEXT,
    ADD COLUMN IF NOT EXISTS last_login_at   TIMESTAMPTZ;

-- 2) Enforce allowed values (kept as TEXT for flexibility)
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_app_user_status'
        ) THEN
            ALTER TABLE app_user
                ADD CONSTRAINT chk_app_user_status
                    CHECK (status IN ('PENDING','ACTIVE','DISABLED'));
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_app_user_auth_provider'
        ) THEN
            ALTER TABLE app_user
                ADD CONSTRAINT chk_app_user_auth_provider
                    CHECK (auth_provider IN ('LOCAL','GOOGLE','GITHUB'));
            -- You can extend this list later with another migration.
        END IF;
    END $$;

-- 3) Make email required (ONLY do this if you have no legacy rows with NULL email)
-- If you *might* have rows already, either backfill them first or skip this and enforce in app-layer for now.
ALTER TABLE app_user
    ALTER COLUMN email SET NOT NULL;

-- 4) Helpful indexes for future OAuth2 + admin lookup
CREATE INDEX IF NOT EXISTS idx_app_user_is_admin
    ON app_user(is_admin);

-- Unique identity for OAuth users (LOCAL users will usually have provider_subject NULL)
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_provider_subject
    ON app_user(auth_provider, provider_subject)
    WHERE provider_subject IS NOT NULL;

-- ----------------------------
-- INVITES: admin invites users (invite-only signup)
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_invite (
                                           id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           email        TEXT NOT NULL,
                                           token_hash   TEXT NOT NULL, -- store sha256 (or bcrypt/argon hash), never store raw token
                                           invited_by   UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,

                                           status       TEXT NOT NULL DEFAULT 'PENDING', -- PENDING / ACCEPTED / EXPIRED / REVOKED
                                           expires_at   TIMESTAMPTZ NOT NULL,
                                           accepted_at  TIMESTAMPTZ,

                                           created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

                                           CONSTRAINT chk_user_invite_status CHECK (status IN ('PENDING','ACCEPTED','EXPIRED','REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_user_invite_email
    ON user_invite(email);

CREATE INDEX IF NOT EXISTS idx_user_invite_status
    ON user_invite(status);

CREATE INDEX IF NOT EXISTS idx_user_invite_expires
    ON user_invite(expires_at);

-- Prevent spamming multiple active invites for the same email
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_invite_pending_email
    ON user_invite(email)
    WHERE status = 'PENDING';

-- Optional: fast lookup for invite acceptance
CREATE INDEX IF NOT EXISTS idx_user_invite_token_hash
    ON user_invite(token_hash);
