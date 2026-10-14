# Demo SPA

Static public OAuth client for the Northstar auth platform. Serve the directory at `http://localhost:4173`; register that exact redirect URI for a public client requiring PKCE S256. The API client sends no secret. In production serve over HTTPS and set a matching strict CSP at the static host.
