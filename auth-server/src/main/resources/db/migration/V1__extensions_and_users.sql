CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "citext";

CREATE TABLE app_role (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(64)  NOT NULL UNIQUE,          -- e.g. ROLE_USER, ROLE_ADMIN
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username               CITEXT       NOT NULL UNIQUE,
    email                  CITEXT       NOT NULL UNIQUE,
    email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    password_hash          VARCHAR(255) NOT NULL,       -- {bcrypt}... via DelegatingPasswordEncoder
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked     BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    password_changed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- OIDC standard claims
    name                   VARCHAR(255),
    given_name             VARCHAR(128),
    family_name            VARCHAR(128),
    preferred_username     VARCHAR(128),
    picture                VARCHAR(1024),
    locale                 VARCHAR(16),
    zoneinfo               VARCHAR(64),
    phone_number           VARCHAR(32),
    phone_number_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    extra_claims           JSONB        NOT NULL DEFAULT '{}'::jsonb,  -- extensible custom claims
    last_login_at          TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_app_user_role_role ON app_user_role(role_id);
