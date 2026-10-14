# Northstar Authorization Platform

Northstar is a Java 21 / Spring Boot OAuth 2.0 and OpenID Connect demo platform. It provides a Spring Authorization Server, a protected resource API, and a browser-only SPA demonstrating Authorization Code with PKCE. PostgreSQL stores users, clients, consents, authorizations, tokens, signing keys, and audit events.

## What it does

- Authenticates users through responsive, accessible, server-rendered login and consent pages.
- Supports OAuth client registration and user administration through admin endpoints.
- Demonstrates Authorization Code + PKCE in a static public SPA, without embedding a client secret.
- Protects a sample API that requires the `profile` scope.
- Stores OAuth state in PostgreSQL and uses Flyway migrations with Hibernate schema validation.
- Encrypts RSA private signing keys at rest, publishes only public key material through JWKS, and supports scheduled key rotation.
- Tracks failed logins for account lockout and detects reuse of rotated refresh tokens.

## Project modules

- `auth-server` — login/consent UI, OAuth/OIDC endpoints, admin API, persistence, and key/token lifecycle.
- `resource-server-demo` — sample protected resource API (`/api/hello`).
- `demo-spa` — static browser application for the PKCE sign-in flow.

For package layout, design details, and the full database schema, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL 15 or newer, with permission to create the `pgcrypto` and `citext` extensions
- A 32-byte encryption key encoded as Base64 for `KEY_ENCRYPTION_SECRET`
- Docker only when running the PostgreSQL Testcontainers integration test

## Configure and run locally

1. Create a PostgreSQL database for the application. Set the following environment variables in your terminal or IDE run configuration. Keep credentials and encryption keys out of source control.

   | Variable | Example for local development | Purpose |
   | --- | --- | --- |
   | `DB_URL` | `jdbc:postgresql://localhost:5432/northstar` | PostgreSQL JDBC URL |
   | `DB_USERNAME` | `northstar` | Database user |
   | `DB_PASSWORD` | your local database password | Database password |
   | `AUTH_ISSUER` | `http://localhost:9000` | Issuer URL advertised by the authorization server |
   | `KEY_ENCRYPTION_SECRET` | Base64 encoding of 32 random bytes | AES-256-GCM key-encryption secret |
   | `ADMIN_BOOTSTRAP_TOKEN` | a long, random local-only token | Bootstrap access to protected admin endpoints |
   | `DEMO_SPA_ORIGIN` | `http://localhost:4173` | Exact browser SPA origin allowed by CORS |

   To generate the encryption value, use a secure random generator and Base64-encode exactly 32 bytes. Preserve the same value across restarts; losing it prevents decryption of stored private keys. Use a secret manager in deployed environments.

2. Run the authorization server with the `dev` profile so localhost HTTP sessions can use non-secure cookies:

   ```powershell
   mvn -pl auth-server spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. In another terminal, run the resource server:

   ```powershell
   mvn -pl resource-server-demo spring-boot:run
   ```

4. Serve the static SPA from the project root:

   ```powershell
   py -m http.server 4173 --directory demo-spa
   ```

5. Create a public SPA client using the admin API, then open `http://localhost:4173`. The registered redirect URI must exactly match `http://localhost:4173/`. The demo's default client ID is `demo-spa`; change the SPA setting if the generated registration has another ID.

   ```powershell
   $body = @{
     clientName = 'Northstar Demo SPA'
     clientType = 'PUBLIC'
     grantTypes = @('authorization_code', 'refresh_token')
     authMethods = @('none')
     scopes = @('openid', 'profile', 'email', 'offline_access')
     redirectUris = @('http://localhost:4173/')
     postLogoutRedirectUris = @()
     requirePkce = $true
     requireConsent = $true
   } | ConvertTo-Json

   Invoke-RestMethod -Method Post `
     -Uri 'http://localhost:9000/admin/clients' `
     -Headers @{ Authorization = "Bearer $env:ADMIN_BOOTSTRAP_TOKEN" } `
     -ContentType 'application/json' -Body $body
   ```

   The client response includes its generated client ID. Public clients do not receive a secret. Keep the bootstrap token private and remove/rotate it after setting up an administrator.

## Useful endpoints

- Authorization server and OpenID discovery: `http://localhost:9000/.well-known/openid-configuration`
- JWKS: `http://localhost:9000/oauth2/jwks`
- Admin clients: `/admin/clients` (create, list, view, delete, rotate secret)
- Admin users: `/admin/users` (create, disable, unlock, assign roles, reset password)
- Resource API: `http://localhost:8080/api/hello` (requires a valid access token with `profile` scope)
- Actuator health: `/actuator/health` on each Spring application

Flyway applies the schema at application startup. Hibernate validates entity mappings against the migrated schema; it does not generate or update tables. Run `mvn -pl auth-server,resource-server-demo -am verify` to compile and run the automated tests. The migration/context Testcontainers test requires Docker and is skipped when Docker is unavailable.

## Deployment notes

Use HTTPS, secure session cookies, a strong managed encryption key, and a secret manager outside local development. Set `AUTH_ISSUER` to the externally visible issuer URL. Configure `DEMO_SPA_ORIGIN` and every redirect URI to exact trusted origins; do not use wildcard origins or redirect URIs. Review CSP and reverse-proxy settings for the deployment topology. See [ARCHITECTURE.md](ARCHITECTURE.md) for implementation boundaries and follow-up hardening work.
