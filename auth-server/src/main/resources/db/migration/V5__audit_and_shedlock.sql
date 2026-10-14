CREATE TABLE audit_event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type  VARCHAR(64)  NOT NULL,   -- LOGIN_SUCCESS, LOGIN_FAILURE, TOKEN_ISSUED, TOKEN_REVOKED,
                                         -- REFRESH_REUSE_DETECTED, CLIENT_CREATED/UPDATED/DELETED,
                                         -- CONSENT_GRANTED, KEY_ROTATED, USER_LOCKED
    principal   VARCHAR(200),
    client_id   VARCHAR(100),
    ip_address  VARCHAR(64),
    user_agent  VARCHAR(512),
    success     BOOLEAN      NOT NULL DEFAULT TRUE,
    details     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created_brin ON audit_event USING BRIN (created_at);
CREATE INDEX idx_audit_type_created ON audit_event(event_type, created_at DESC);
CREATE INDEX idx_audit_principal    ON audit_event(principal, created_at DESC);
CREATE INDEX idx_audit_client       ON audit_event(client_id, created_at DESC);

-- ShedLock: ensures scheduled jobs run once across multiple instances
CREATE TABLE shedlock (
    name       VARCHAR(64)  PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
