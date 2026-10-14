-- Scope catalog + which OIDC claims each scope releases (data-driven userinfo/ID token)
CREATE TABLE oauth_scope (
    name        VARCHAR(100) PRIMARY KEY,               -- openid, profile, email, offline_access, custom
    description VARCHAR(255) NOT NULL,
    is_openid   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE oauth_scope_claim (
    scope_name VARCHAR(100) NOT NULL REFERENCES oauth_scope(name) ON DELETE CASCADE,
    claim_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (scope_name, claim_name)
);

CREATE TABLE oauth_client (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id                     VARCHAR(100)  NOT NULL UNIQUE,
    client_secret_hash            VARCHAR(255),          -- NULL for public clients
    client_id_issued_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    client_secret_expires_at      TIMESTAMPTZ,
    client_name                   VARCHAR(200)  NOT NULL,
    client_type                   VARCHAR(16)   NOT NULL CHECK (client_type IN ('CONFIDENTIAL','PUBLIC')),
    require_pkce                  BOOLEAN       NOT NULL DEFAULT TRUE,
    require_consent               BOOLEAN       NOT NULL DEFAULT TRUE,
    access_token_format           VARCHAR(16)   NOT NULL DEFAULT 'SELF_CONTAINED'
                                  CHECK (access_token_format IN ('SELF_CONTAINED','REFERENCE')),
    access_token_ttl_seconds      INT           NOT NULL DEFAULT 600,
    refresh_token_ttl_seconds     INT           NOT NULL DEFAULT 1209600,
    authorization_code_ttl_seconds INT          NOT NULL DEFAULT 300,
    reuse_refresh_tokens          BOOLEAN       NOT NULL DEFAULT FALSE,
    id_token_signature_alg        VARCHAR(16)   NOT NULL DEFAULT 'RS256',
    enabled                       BOOLEAN       NOT NULL DEFAULT TRUE,
    extra_settings                JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_by                    VARCHAR(100),
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_public_no_secret CHECK (client_type <> 'PUBLIC' OR client_secret_hash IS NULL),
    CONSTRAINT chk_public_pkce      CHECK (client_type <> 'PUBLIC' OR require_pkce = TRUE)
);

CREATE TABLE oauth_client_auth_method (
    client_pk UUID        NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    method    VARCHAR(64) NOT NULL,   -- client_secret_basic, client_secret_post, none, private_key_jwt
    PRIMARY KEY (client_pk, method)
);

CREATE TABLE oauth_client_grant_type (
    client_pk  UUID        NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    grant_type VARCHAR(64) NOT NULL
               CHECK (grant_type IN ('authorization_code','refresh_token','client_credentials')),
    PRIMARY KEY (client_pk, grant_type)
);

CREATE TABLE oauth_client_redirect_uri (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_pk  UUID          NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    uri        VARCHAR(2048) NOT NULL,
    uri_type   VARCHAR(24)   NOT NULL DEFAULT 'REDIRECT'
               CHECK (uri_type IN ('REDIRECT','POST_LOGOUT_REDIRECT')),
    UNIQUE (client_pk, uri, uri_type)
);
CREATE INDEX idx_client_redirect_client ON oauth_client_redirect_uri(client_pk);

CREATE TABLE oauth_client_scope (
    client_pk  UUID         NOT NULL REFERENCES oauth_client(id) ON DELETE CASCADE,
    scope_name VARCHAR(100) NOT NULL REFERENCES oauth_scope(name),
    PRIMARY KEY (client_pk, scope_name)
);
CREATE INDEX idx_client_scope_scope ON oauth_client_scope(scope_name);
