-- One row per authorization grant (a login/consent session for client + user)
CREATE TABLE oauth_authorization (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_pk                UUID         NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    principal_name           VARCHAR(200) NOT NULL,        -- username, or client_id for client_credentials
    user_id                  UUID         REFERENCES app_user(id) ON DELETE CASCADE,  -- NULL for client_credentials
    grant_type               VARCHAR(64)  NOT NULL,
    authorized_scopes        TEXT[]       NOT NULL DEFAULT '{}',
    attributes               JSONB        NOT NULL DEFAULT '{}'::jsonb,   -- serialized OAuth2Authorization attributes
    state_hash               CHAR(64),                      -- SHA-256 of the `state` param
    family_id                UUID         NOT NULL DEFAULT gen_random_uuid(),  -- refresh-token family for reuse detection
    revoked                  BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at               TIMESTAMPTZ,
    revoked_reason           VARCHAR(100),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                  BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_authz_client_principal ON oauth_authorization(client_pk, principal_name);
CREATE INDEX idx_authz_user             ON oauth_authorization(user_id);
CREATE INDEX idx_authz_state_hash       ON oauth_authorization(state_hash) WHERE state_hash IS NOT NULL;
CREATE INDEX idx_authz_family           ON oauth_authorization(family_id);

-- One row per issued token. Lookup by hash; value kept for rebuilding Spring token objects.
CREATE TABLE oauth_token (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    authorization_id UUID         NOT NULL REFERENCES oauth_authorization(id) ON DELETE CASCADE,
    token_type       VARCHAR(32)  NOT NULL
                     CHECK (token_type IN ('AUTHORIZATION_CODE','ACCESS_TOKEN','REFRESH_TOKEN','ID_TOKEN')),
    value            TEXT         NOT NULL,
    value_hash       CHAR(64)     NOT NULL,                -- hex SHA-256(value)
    jti              VARCHAR(64),                          -- for JWT access/id tokens
    scopes           TEXT[]       NOT NULL DEFAULT '{}',
    metadata         JSONB        NOT NULL DEFAULT '{}'::jsonb,  -- serialized token metadata / ID token claims
    issued_at        TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL,
    invalidated      BOOLEAN      NOT NULL DEFAULT FALSE,  -- revoked / used
    invalidated_at   TIMESTAMPTZ,
    rotated_to_id    UUID REFERENCES oauth_token(id),      -- refresh rotation chain
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_token_type_hash ON oauth_token(token_type, value_hash);
CREATE INDEX idx_token_authorization  ON oauth_token(authorization_id);
CREATE INDEX idx_token_jti            ON oauth_token(jti) WHERE jti IS NOT NULL;
CREATE INDEX idx_token_expires_at     ON oauth_token(expires_at);   -- cleanup job

CREATE TABLE oauth_authorization_consent (
    client_pk      UUID         NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    principal_name VARCHAR(200) NOT NULL,
    authorities    TEXT[]       NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (client_pk, principal_name)
);
