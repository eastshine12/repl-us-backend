# Social Login Setup Runbook

This runbook describes how to configure Google and Apple social-login
validation for the repl.us backend. It is safe for a public repository: it
documents variable names, verification checks, and troubleshooting paths, not
real client IDs, provider tokens, client secrets, private keys, or production
endpoints.

## Goal

The backend accepts provider ID tokens at `/api/auth/social`, validates them,
creates or reuses a backend user, and returns a backend bearer session. The
deployment must be configured with the provider audiences that are allowed to
sign in.

The first production social-login slice supports:

- Google ID tokens.
- Apple identity tokens.
- Backend-owned bearer sessions after provider verification.

## Backend Contract

Clients call:

```http
POST /api/auth/social
Content-Type: application/json

{
  "provider": "GOOGLE",
  "providerToken": "<provider-id-token>"
}
```

Use `provider: "APPLE"` for Apple identity tokens. The provider value is
case-insensitive.

The backend verifies:

- Token signature against the provider JWKS.
- Issuer.
- Expiration.
- Audience against the configured client IDs.

Default provider metadata:

| Provider | Issuer | JWKS |
| --- | --- | --- |
| Google | `https://accounts.google.com` | `https://www.googleapis.com/oauth2/v3/certs` |
| Apple | `https://appleid.apple.com` | `https://appleid.apple.com/auth/keys` |

Accepted audiences are configured with comma-separated environment variables:

```text
REPLUS_AUTH_SOCIAL_GOOGLE_CLIENT_IDS=<google-web-client-id>,<google-ios-client-id>
REPLUS_AUTH_SOCIAL_APPLE_CLIENT_IDS=<apple-service-id-or-bundle-id>
```

If a provider has no configured client IDs, that provider fails closed with
`401 UNAUTHENTICATED`.

## Google Setup

1. In Google Cloud, create or confirm the OAuth consent configuration for the
   app.
2. Create OAuth clients for every app surface that can mint ID tokens for the
   backend, such as web and iOS.
3. Add every accepted Google client ID to `REPLUS_AUTH_SOCIAL_GOOGLE_CLIENT_IDS`.
4. Make sure the client sends a Google ID token to the backend.

Do not send a Google user ID, access token, or authorization code to
`/api/auth/social`. The backend expects an ID token.

## Apple Setup

1. In the Apple Developer account, enable Sign in with Apple for the relevant
   app identifier or service ID.
2. Confirm which audience value appears in the Apple identity token for the
   client being tested. Native and web flows can use different audience values.
3. Add every accepted Apple audience to `REPLUS_AUTH_SOCIAL_APPLE_CLIENT_IDS`.
4. Make sure the client sends the Apple identity token to the backend.

Do not send an Apple authorization code or access token to `/api/auth/social`.
The backend expects an identity token.

## Render Setup

Set these values in the Render web service environment:

```text
REPLUS_AUTH_SOCIAL_GOOGLE_CLIENT_IDS=<google-web-client-id>,<google-ios-client-id>
REPLUS_AUTH_SOCIAL_APPLE_CLIENT_IDS=<apple-service-id-or-bundle-id>
```

Deploy or restart the service after changing them.

Check the public app metadata without exposing actual client IDs:

```bash
curl -fsS https://<api-host>/actuator/info
```

Expected fields:

```json
{
  "auth": {
    "social": {
      "providers": {
        "google": {
          "clientIdsConfigured": true
        },
        "apple": {
          "clientIdsConfigured": true
        }
      }
    }
  }
}
```

## Smoke Checks

Validate that the endpoint exists and rejects invalid tokens:

```bash
scripts/smoke-api.sh --with-social-auth-failure https://<api-host>
```

This smoke check is safe for production because it does not create users or
sessions.

After setting both Google and Apple client ID environment variables, validate
that the deployed backend sees them without sending a real provider token:

```bash
scripts/smoke-api.sh --expect-social-client-ids-configured https://<api-host>
```

Validate the full login session flow with a short-lived test ID token:

```bash
export SMOKE_SOCIAL_AUTH_PROVIDER=google
read -r -s SMOKE_SOCIAL_AUTH_TOKEN
export SMOKE_SOCIAL_AUTH_TOKEN
scripts/smoke-api.sh --with-social-auth-success https://<api-host>
unset SMOKE_SOCIAL_AUTH_TOKEN
```

Use `SMOKE_SOCIAL_AUTH_PROVIDER=apple` when validating an Apple identity token.
The provider value is case-insensitive. Do not commit or paste the token into
issues, PRs, deployment logs, or chat.

## Failure Guide

`/api/auth/social` returns `401 UNAUTHENTICATED`:

- The provider client IDs are missing or blank.
- The token audience does not match any configured client ID.
- The client sent an access token, authorization code, or user ID instead of an
  ID token.
- The token is expired.
- The token provider does not match the request provider.
- The issuer or JWKS endpoint does not match the expected provider metadata.

`/actuator/info` shows `clientIdsConfigured: false`:

- The environment variable is missing or blank.
- The service was not redeployed or restarted after the variable changed.
- The value was added to the wrong Render service or environment.

The success smoke receives a backend token but `/api/me` fails:

- Check backend session persistence.
- Confirm `REPLUS_AUTH_SESSION_STORE=database` in production.
- Inspect application logs for session-store or database errors.

## Security Notes

- Never commit provider ID tokens, auth codes, client secrets, private keys, or
  production session tokens.
- Client IDs are not secrets, but keep them in the hosting dashboard so deploy
  configuration stays environment-specific.
- If a future flow needs auth-code exchange or client-secret signing, keep those
  secrets in the platform secret manager and document only variable names here.

## Official References

- [Google backend authentication guide](https://developers.google.com/identity/sign-in/web/backend-auth)
- [Apple Sign in with Apple REST API](https://developer.apple.com/documentation/signinwithapplerestapi)
- [Apple OpenID Connect discovery document](https://appleid.apple.com/.well-known/openid-configuration)
