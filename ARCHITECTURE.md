# Architecture and Database Reference

This document describes the current implementation in the repository. Flyway migrations under `auth-server/src/main/resources/db/migration` are the source of truth for SQL types and constraints; JPA entities are validated against them at startup (`spring.jpa.hibernate.ddl-auto=validate`).

## Repository layout

```text
E:\Projects\
├── auth-server/
│   └── src/main/
│       ├── java/com/example/authserver/
│       │   ├── config/          Spring Security, JWK, password, scheduling, properties
│       │   ├── domain/          JPA entities grouped by user/client/auth/key/audit
│       │   ├── repository/      Spring Data JPA persistence interfaces
│       │   ├── security/        Admin bootstrap authentication filter
│       │   ├── service/         Application services and SAS persistence adapters
│       │   ├── validation/      Composable client-registration rules
│       │   └── web/             MVC controllers, admin DTOs, login and consent views
│       └── resources/
│           ├── db/migration/    Versioned Flyway schema and seed data
│           ├── static/          Locally served CSS and browser assets
│           └── templates/       Server-rendered login and consent pages
├── resource-server-demo/        Separate Spring resource-server example
├── demo-spa/                    Static Authorization Code + PKCE client
├── pom.xml                      Maven parent and module list
└── ARCHITECTURE.md               This document
```

### Feature and adapter packages

- `domain.user`: `AppUser` and `AppRole`, including login state, OIDC user claims, and role membership.
- `domain.client`: persisted clients, redirect URIs, and scope catalog.
- `domain.authorization`: grants, token records, and consent authorities.
- `domain.key`: signing-key metadata and encrypted private-key bytes.
- `domain.audit`: audit event entity.
- `repository`: database access contracts. Services depend on these interfaces rather than constructing SQL in web controllers.
- `service.client`: client registration use cases, RegisteredClient conversion, and a caching decorator.
- `service.user`: Spring Security user lookup, administration, lockout, and authentication auditing.
- `service.authorization`: Spring Authorization Server persistence adapters, OAuth object mapping, refresh-token family operations, and token hashing.
- `service.key`: RSA generation, encryption, key retrieval, and rotation.
- `validation`: one rule per client-registration concern, coordinated by `ClientRegistrationValidator`.
- `web`: thin HTTP adapters and request/response DTOs; controllers delegate to application services.
- `config`: framework wiring for the authorization server, password encoder, JWK source, scheduling, and persistence auditing.

## Design patterns and principles

### Ports/adapters boundary

Spring Authorization Server defines `RegisteredClientRepository`, `OAuth2AuthorizationService`, and `OAuth2AuthorizationConsentService` contracts. `JpaRegisteredClientRepository`, `JpaOAuth2AuthorizationService`, and `JpaOAuth2AuthorizationConsentService` implement those contracts and translate protocol objects to/from application persistence. This keeps most persistence logic behind adapters while allowing the Spring Authorization Server to own protocol processing.

### Repository pattern

Spring Data interfaces (`AppUserRepository`, `OAuthClientRepository`, `OAuthAuthorizationRepository`, and others) isolate entity persistence and named queries from application behavior. Transactional application services coordinate multi-repository changes.

### Application/service layer

`ClientAdminService`, `UserAdminService`, `LoginAttemptService`, `RefreshTokenFamilyService`, and `KeyRotationService` own use cases. MVC controllers do request binding and response mapping; they do not implement database transactions or the OAuth protocol.

### Chain of Responsibility / Specification-style validation

`ClientRegistrationValidator` runs a list of `ClientRegistrationRule` implementations. Redirect URI, grant type, client type, scope, and secret policy checks are separate rules, which can be extended or tested independently. This structure supports single-responsibility validation; the rules are currently run as a coordinated validation chain rather than being Spring Data `Specification` predicates.

### Decorator and Factory

`CachingRegisteredClientRepository` decorates the persistent registered-client repository with a Caffeine cache. `RegisteredClientFactory` converts between the OAuth client entity and Spring Authorization Server's `RegisteredClient` model so mapping logic is centralized.

### Strategy and dependency inversion

`KeyGenerator`, `KeyEncryptor`, and `KeyProvider` are interfaces with RSA, AES-GCM, and database-backed implementations. Rotation logic depends on abstractions and receives cryptographic/persistence operations through constructor injection.

### Event-driven auditing

Spring Security authentication success/failure events are observed by `AuthenticationAuditListener`. It delegates lockout bookkeeping and emits audit records without coupling the authentication provider to audit table details.

### Transactional consistency and concurrency controls

Service-level transactions keep coupled changes together. Optimistic version columns protect user, client, and authorization rows from lost updates. ShedLock and PostgreSQL advisory locks coordinate scheduled key rotation across application instances. Token values are indexed by a SHA-256 hash, and refresh tokens are grouped by a family identifier for reuse response.

### Security boundaries

Public clients must use PKCE; redirect URIs are validated; CORS is configured for a specific SPA origin. Client secrets use BCrypt. Signing-key private material is AES-GCM encrypted in the database, while the public JWKS source only publishes public keys. Bootstrap admin authentication is isolated in a filter and accepts a configured bearer token.

## Database schema

All timestamps use PostgreSQL `TIMESTAMPTZ` unless stated otherwise. `UUID` values are generated with `gen_random_uuid()` where the migration defines a default. JSON columns are `JSONB`; string collections use `TEXT[]` or join tables. `ON DELETE CASCADE` relationships are noted below. Index names and detailed SQL are present in the migration files.

### V1 — extensions and users

Enables `pgcrypto` (UUID generation) and `citext` (case-insensitive usernames/emails).

#### `app_role`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `name` | VARCHAR(64) | Required, unique, e.g. `ROLE_USER`, `ROLE_ADMIN` |
| `description` | VARCHAR(255) | Optional description |
| `created_at` | TIMESTAMPTZ | Required, defaults to current time |

#### `app_user`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `username` | CITEXT | Required, unique, case-insensitive login identifier |
| `email` | CITEXT | Required, unique, case-insensitive |
| `email_verified` | BOOLEAN | Required, defaults false |
| `password_hash` | VARCHAR(255) | Required, encoded password (DelegatingPasswordEncoder format) |
| `enabled` | BOOLEAN | Required, defaults true |
| `account_non_locked` | BOOLEAN | Required, defaults true |
| `failed_login_attempts` | INT | Required, defaults zero |
| `locked_until` | TIMESTAMPTZ | Optional time-based lock expiry |
| `password_changed_at` | TIMESTAMPTZ | Required, defaults current time |
| `name`, `given_name`, `family_name` | VARCHAR(255/128/128) | Optional OIDC profile claims |
| `preferred_username` | VARCHAR(128) | Optional OIDC claim |
| `picture` | VARCHAR(1024) | Optional OIDC claim URL |
| `locale` | VARCHAR(16) | Optional OIDC claim |
| `zoneinfo` | VARCHAR(64) | Optional OIDC claim |
| `extra_claims` | JSONB | Required custom claims, defaults to empty object |
| `last_login_at` | TIMESTAMPTZ | Optional last successful login |
| `created_at`, `updated_at` | TIMESTAMPTZ | Required, defaults current time |
| `version` | BIGINT | Required, defaults zero; optimistic locking |

#### `app_user_role`

Join table with composite primary key (`user_id`, `role_id`). Both UUID columns reference their parent tables and cascade on delete. An index on `role_id` supports reverse role lookup.

### V2 — scopes and clients

#### `oauth_scope`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `name` | VARCHAR(100) | Primary key, e.g. `openid`, `profile`, `email` |
| `description` | VARCHAR(255) | Required human-readable description |
| `is_openid` | BOOLEAN | Required, defaults false |
| `created_at` | TIMESTAMPTZ | Required, defaults current time |

#### `oauth_scope_claim`

Composite primary key (`scope_name`, `claim_name`). `scope_name` references `oauth_scope(name)` with cascade on delete. Defines which claims are associated with each scope.

#### `oauth_client`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `client_id` | VARCHAR(100) | Required, unique public OAuth identifier |
| `client_secret_hash` | VARCHAR(255) | Nullable (public clients have no secret); hashed secret for confidential clients |
| `client_id_issued_at`, `client_secret_expires_at` | TIMESTAMPTZ | Issue/expiry dates; secret expiry nullable |
| `client_name` | VARCHAR(200) | Required display name |
| `client_type` | VARCHAR(16) | Required, `CONFIDENTIAL` or `PUBLIC` |
| `require_pkce` | BOOLEAN | Required, defaults true; public clients must require PKCE |
| `require_consent` | BOOLEAN | Required, defaults true |
| `access_token_format` | VARCHAR(16) | `SELF_CONTAINED` or `REFERENCE`, defaults `SELF_CONTAINED` |
| `access_token_ttl_seconds` | INT | Required; defaults 600 |
| `refresh_token_ttl_seconds` | INT | Required; defaults 1209600 |
| `authorization_code_ttl_seconds` | INT | Required; defaults 300 |
| `reuse_refresh_tokens` | BOOLEAN | Required, defaults false (rotate rather than reuse) |
| `id_token_signature_alg` | VARCHAR(16) | Required, defaults `RS256` |
| `enabled` | BOOLEAN | Required, defaults true |
| `extra_settings` | JSONB | Required settings object, defaults empty |
| `created_by` | VARCHAR(100) | Optional registering administrator |
| `created_at`, `updated_at` | TIMESTAMPTZ | Required, defaults current time |
| `version` | BIGINT | Required, defaults zero; optimistic locking |

Checks constrain client type and access-token format. Additional checks prohibit a public client secret and require PKCE for public clients.

#### `oauth_client_auth_method`

Composite primary key (`client_pk`, `method`); `client_pk` references the client with cascade on delete. Stores supported client authentication methods such as `client_secret_basic` or `none`.

#### `oauth_client_grant_type`

Composite primary key (`client_pk`, `grant_type`); client foreign key cascades on delete. Grant type is constrained to `authorization_code`, `refresh_token`, or `client_credentials`.

#### `oauth_client_redirect_uri`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `client_pk` | UUID | Required client foreign key; cascades on delete |
| `uri` | VARCHAR(2048) | Required exact redirect or post-logout URI |
| `uri_type` | VARCHAR(24) | `REDIRECT` or `POST_LOGOUT_REDIRECT`, defaults `REDIRECT` |

Unique constraint on (`client_pk`, `uri`, `uri_type`); indexed by `client_pk`.

#### `oauth_client_scope`

Composite primary key (`client_pk`, `scope_name`). Client deletion cascades; `scope_name` references the scope catalog. An index supports lookup by scope.

### V3 — authorizations, tokens, and consent

#### `oauth_authorization`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `client_pk` | UUID | Required client foreign key; cascades on client delete |
| `principal_name` | VARCHAR(200) | Required user name or client ID for client credentials |
| `user_id` | UUID | Nullable user foreign key; cascades when user is deleted |
| `grant_type` | VARCHAR(64) | Required authorization grant type |
| `authorized_scopes` | TEXT[] | Required, defaults empty array |
| `attributes` | JSONB | Required serialized authorization attributes, defaults empty object |
| `state_hash` | CHAR(64) | Optional SHA-256 hash of OAuth state |
| `family_id` | UUID | Required refresh-token family identifier, generated by default |
| `revoked` | BOOLEAN | Required, defaults false |
| `revoked_at` | TIMESTAMPTZ | Optional revocation time |
| `revoked_reason` | VARCHAR(100) | Optional reason |
| `created_at`, `updated_at` | TIMESTAMPTZ | Required, defaults current time |
| `version` | BIGINT | Required, defaults zero; optimistic locking |

Indexes support client/principal, user, non-null state hashes, and token-family queries.

#### `oauth_token`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | UUID | Primary key, generated by default |
| `authorization_id` | UUID | Required authorization foreign key; cascades on delete |
| `token_type` | VARCHAR(32) | Required: `AUTHORIZATION_CODE`, `ACCESS_TOKEN`, `REFRESH_TOKEN`, or `ID_TOKEN` |
| `value` | TEXT | Required token value used to reconstruct framework token objects |
| `value_hash` | CHAR(64) | Required hexadecimal SHA-256 hash, used for lookup |
| `jti` | VARCHAR(64) | Optional JWT identifier |
| `scopes` | TEXT[] | Required, defaults empty array |
| `metadata` | JSONB | Required token metadata/claims, defaults empty object |
| `issued_at`, `expires_at` | TIMESTAMPTZ | Required validity interval |
| `invalidated` | BOOLEAN | Required, defaults false; includes consumed/used tokens |
| `invalidated_at` | TIMESTAMPTZ | Optional invalidation time |
| `rotated_to_id` | UUID | Optional self-reference to replacement token |
| `created_at` | TIMESTAMPTZ | Required, defaults current time |

Unique index on (`token_type`, `value_hash`); additional indexes on authorization, non-null JTI, and expiry support token lookup and cleanup.

#### `oauth_authorization_consent`

Composite primary key (`client_pk`, `principal_name`). `client_pk` references the client with cascade on delete. `authorities` is a required `TEXT[]` defaulting to empty; `created_at` and `updated_at` are required timestamps defaulting to current time.

### V4 — signing keys

#### `signing_key`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `kid` | VARCHAR(64) | Primary key and JWK key identifier |
| `algorithm` | VARCHAR(16) | Required, defaults `RS256` |
| `key_type` | VARCHAR(8) | Required `RSA` or `EC`, defaults `RSA` |
| `public_key_pem` | TEXT | Required public key material |
| `private_key_encrypted` | BYTEA | Required AES-256-GCM encrypted PKCS#8 private key, IV prefixed |
| `status` | VARCHAR(16) | Required `PENDING`, `ACTIVE`, `RETIRING`, or `RETIRED`; defaults `ACTIVE` |
| `created_at` | TIMESTAMPTZ | Required, defaults current time |
| `activated_at`, `retire_at`, `expires_at` | TIMESTAMPTZ | Optional lifecycle timestamps; expiry ends JWKS publication |

An index supports status lookup. A partial unique index allows only one row whose status is `ACTIVE`.

### V5 — audit and distributed locks

#### `audit_event`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `id` | BIGINT | Identity primary key |
| `event_type` | VARCHAR(64) | Required event name, e.g. login, token reuse, client change, key rotation |
| `principal` | VARCHAR(200) | Optional user/service principal |
| `client_id` | VARCHAR(100) | Optional OAuth client ID |
| `ip_address` | VARCHAR(64) | Optional originating address |
| `user_agent` | VARCHAR(512) | Optional user-agent string |
| `success` | BOOLEAN | Required, defaults true |
| `details` | JSONB | Required details object, defaults empty |
| `created_at` | TIMESTAMPTZ | Required, defaults current time |

Indexes support time-window, event-type, principal, and client queries. The migration defines indexes but does not itself enforce append-only behavior; database permissions can be tightened for production.

#### `shedlock`

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `name` | VARCHAR(64) | Primary key; scheduled task lock name |
| `lock_until` | TIMESTAMPTZ | Required lock expiry |
| `locked_at` | TIMESTAMPTZ | Required acquisition timestamp |
| `locked_by` | VARCHAR(255) | Required application instance identifier |

Used by ShedLock to ensure scheduled work such as signing-key rotation runs on only one instance at a time.

### V6 — seed data

Seeds roles `ROLE_USER` and `ROLE_ADMIN`; scopes `openid`, `profile`, `email`, and `offline_access`; and standard OIDC claim associations for the seeded scopes.

## Configuration and runtime behavior

The authorization server listens on port `9000`; the resource-server demo defaults to `8080`. Required runtime values are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AUTH_ISSUER`, `KEY_ENCRYPTION_SECRET`, and `ADMIN_BOOTSTRAP_TOKEN`. `DEMO_SPA_ORIGIN` defaults to localhost port 4173. The `dev` profile disables secure-cookie-only behavior for local HTTP; production should retain secure cookies and HTTPS. A `redis` profile is available for Spring Session storage.

Flyway runs migrations before Hibernate validation. The app does not use `ddl-auto:update` or Hibernate schema generation. The Testcontainers integration test uses an isolated PostgreSQL container and does not connect to a developer database.

## Suggested next improvements

1. Run the PostgreSQL migration/context test and add Docker-backed end-to-end coverage for PKCE, consent, token rotation/reuse, revocation, discovery, and JWKS.
2. Add explicit method-level authorization policy for each admin action and rate limiting for login/bootstrap routes.
3. Enforce append-only audit access at the database-role level; introduce retention/partitioning policy if audit volume grows.
4. Add readiness checks and metrics for database/key availability, lockouts, token reuse, and rotation outcomes.
5. Add architecture tests to keep framework/web concerns out of domain and application packages.

