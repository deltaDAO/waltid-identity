# Using JWT with ktor-authnz

The `jwt` auth method allows clients to authenticate by posting a signed JWT directly — no browser redirect required. It supports two verification modes:

- **JWKS** (asymmetric, e.g. RS256 / ES256) — for tokens issued by external providers such as Hanko
- **HMAC** (symmetric) — for tokens signed with a shared secret

---

## Configuration

Add a `jwt` entry to `authFlows` in `ktor-authnz.conf`:

### Option A — JWKS verification (Hanko, Keycloak, any OIDC provider)

```hocon
{
    method: "jwt"
    config: {
        jwksUrl: "https://YOUR_PROJECT_ID.hanko.io/.well-known/jwks.json"
        issuer: "https://YOUR_PROJECT_ID.hanko.io"   # optional but recommended
        identifyClaim: "sub"                          # default
    }
    ok: true
}
```

The `kid` header in the incoming JWT is used to select the matching key from the JWKS endpoint. Keys are cached in memory; restart `wallet-api` if keys are rotated.

### Option B — HMAC verification (symmetric secret)

```hocon
{
    method: "jwt"
    config: {
        verifyKey: "your-shared-secret"
        identifyClaim: "sub"
    }
    ok: true
}
```

### Config fields

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `jwksUrl` | Yes (if no `verifyKey`) | — | JWKS endpoint URL for asymmetric key verification |
| `verifyKey` | Yes (if no `jwksUrl`) | — | Symmetric HMAC secret string |
| `issuer` | No | — | Expected `iss` claim value; validation skipped if omitted |
| `identifyClaim` | No | `sub` | JWT claim used as the account identifier |

Exactly one of `jwksUrl` or `verifyKey` must be set.

---

## Request flow

```
POST /auth/account/jwt
Content-Type: text/plain

eyJhbGci...<signed-jwt>
```

1. ktor-authnz parses the JWT header to extract `kid` (JWKS mode) or proceeds directly (HMAC mode).
2. The signature is verified against the JWKS public key or the HMAC secret.
3. If `issuer` is configured, the `iss` claim must match exactly.
4. The `exp` claim is checked — expired tokens are rejected (JWKS mode only; HMAC mode relies on the claim being present in the token).
5. The value of `identifyClaim` (default `sub`) is extracted and used to look up or create the wallet account.
6. A wallet session cookie is set and `AuthSessionInformation` is returned.

---

## Example — Hanko Cloud

After a user authenticates with Hanko (via the Hanko API or SDK), post their session token to the wallet:

```bash
HANKO_JWT="eyJhbGci..."   # obtained from Hanko

curl -X POST http://localhost:7001/wallet-api/auth/account/jwt \
  -H "Content-Type: text/plain" \
  --data-raw "$HANKO_JWT"
```

Response:
```json
{
  "session_id": "c3d4e5f6-...",
  "status": "SUCCESS",
  "token": "a1b2c3d4-...",
  "expiration": "2025-12-04T12:12:18Z"
}
```

The wallet account created via this flow is identical to the one produced by the OIDC browser flow for the same Hanko `sub` — both flows converge on the same account.

---

## Combining with other auth flows

The `jwt` flow can coexist with `oidc`, `email`, or any other flow in `authFlows`. Each flow registers its own route; they do not interfere with each other.

```hocon
authFlows = [
    {
        method: "oidc"
        config: { ... }
        ok: true
    },
    {
        method: "jwt"
        config: {
            jwksUrl: "https://YOUR_PROJECT_ID.hanko.io/.well-known/jwks.json"
            issuer: "https://YOUR_PROJECT_ID.hanko.io"
        }
        ok: true
    }
]
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 401 — signature invalid | Verify `jwksUrl` is reachable from the server; check that `kid` in the JWT header matches a key in the JWKS |
| 401 — invalid issuer | The `issuer` config value must exactly match the `iss` claim (check for trailing slashes) |
| 401 — token expired | Obtain a fresh token; Hanko session tokens are short-lived |
| 401 — missing `kid` | JWKS mode requires the JWT header to contain a `kid` field |
| JWKS stale after key rotation | Keys are cached in memory; restart the service to force a refresh |
