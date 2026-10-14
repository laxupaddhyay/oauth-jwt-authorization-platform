CREATE TABLE signing_key (
    kid                    VARCHAR(64) PRIMARY KEY,
    algorithm              VARCHAR(16)  NOT NULL DEFAULT 'RS256',
    key_type               VARCHAR(8)   NOT NULL DEFAULT 'RSA' CHECK (key_type IN ('RSA','EC')),
    public_key_pem         TEXT         NOT NULL,
    private_key_encrypted  BYTEA        NOT NULL,        -- AES-256-GCM(private PKCS8), iv prefixed
    status                 VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('PENDING','ACTIVE','RETIRING','RETIRED')),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    activated_at           TIMESTAMPTZ,
    retire_at              TIMESTAMPTZ,                  -- stop signing with it after this
    expires_at             TIMESTAMPTZ                   -- stop publishing in JWKS after this
);
CREATE INDEX idx_signing_key_status ON signing_key(status);
-- Only one ACTIVE signing key at a time
CREATE UNIQUE INDEX uq_signing_key_single_active ON signing_key(status) WHERE status = 'ACTIVE';
