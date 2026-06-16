# Deployment Runbook

This runbook describes the minimum production setup for the repl.us backend.
It is safe for a public repository: it documents variable names and checks, not
real secrets, tokens, database URLs, or private service endpoints.

## Deployment Model

The backend is a Kotlin Spring Boot application that serves the REST API and
operational health endpoints. A production deployment needs:

- Java 21 runtime.
- PostgreSQL-compatible database.
- S3-compatible object storage when response video uploads are enabled.
- A platform-provided secret store for environment variables.
- HTTPS termination at the platform, load balancer, or reverse proxy.

The application exposes:

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- Public application metadata: `/actuator/info`

Readiness includes the application readiness state, database health, and storage
configuration health.

## Container Image

The repository includes a platform-neutral `Dockerfile` for deployment targets
that can run container images. The image:

- Builds the application with the Gradle wrapper.
- Copies only the Spring Boot jar into the runtime stage.
- Runs on Java 21 JRE.
- Uses the `prod` Spring profile by default.
- Runs as a non-root application user.

Build locally:

```bash
docker build -t repl-us-backend:local .
```

CI also runs a local-profile smoke test against the built image before the
Docker image build job can pass.

The production image is expected to fail fast when required production variables
are missing. For a local container smoke test without real production services,
override the profile explicitly:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  repl-us-backend:local
```

## Render Blueprint

The repository includes a `render.yaml` blueprint for an initial Render web
service and PostgreSQL database. The web service uses the Dockerfile, points the
health check at `/actuator/health/readiness`, and keeps secrets out of git with
`sync: false` environment variables. The blueprint starts on free plans to avoid
surprise spend during the first smoke deployment. Review the plans before
serving real users.

The web service is configured to auto-deploy only after the linked GitHub
checks pass. This keeps a failing build or Docker smoke regression from
reaching the Render environment just because it was merged to `main`.

Before applying the blueprint, prepare:

- Explicit HTTPS frontend origins for `REPLUS_WEB_CORS_ALLOWED_ORIGINS`.
- Google and Apple app client IDs for social-login validation.
- A storage mode for the target environment. Use local storage only for the
  first backend smoke deploy; use object storage for upload-flow validation.
- Object storage values for response-video uploads when object storage is used.

The blueprint injects the Render PostgreSQL connection string as `DATABASE_URL`.
The application converts Render's `postgresql://...` value into Spring
datasource properties during startup.

After the blueprint creates the service, set every `sync: false` value in the
Render Dashboard before expecting the service to become healthy. Missing values
are intentionally caught by the production guard during startup.
Google and Apple social-login client IDs are also `sync: false` values so they
can be added through the Render Dashboard without committing app identifiers to
the repository.

## Render First Deploy Checklist

Use this checklist for the first Render smoke deployment.

1. Open Render and create a new Blueprint from the GitHub repository.
2. Select the repository and confirm Render detected `render.yaml`.
3. Keep the generated web service and PostgreSQL database on the free plans for
   the first smoke deploy.
4. Confirm the web service has a `DATABASE_URL` env var populated from the
   generated `repl-us-postgres` database.
5. Set every `sync: false` environment variable in the Render Dashboard.
6. Trigger the first deploy.
7. Watch the build log until the Docker image build completes.
8. Watch the runtime log for production guard failures.
9. Check the health endpoints after the service starts.

Minimum values for a backend-only smoke deploy:

```text
REPLUS_WEB_CORS_ALLOWED_ORIGINS=<https-frontend-origin>
REPLUS_WEB_BASE_URL=<https-frontend-origin>
REPLUS_AUTH_SESSION_STORE=database
REPLUS_STORAGE_MODE=local
```

Use this only for smoke testing health checks and non-upload API flows. Switch
back to object storage before validating response-video upload flows.

For upload-flow validation, keep `REPLUS_STORAGE_MODE=object-storage` and set:

```text
REPLUS_STORAGE_OBJECT_BUCKET=<bucket-name>
REPLUS_STORAGE_OBJECT_PUBLIC_BASE_URL=<public-playback-base-url>
REPLUS_STORAGE_OBJECT_REGION=<region-or-provider-region>
REPLUS_STORAGE_OBJECT_ENDPOINT=<provider-endpoint-if-required>
REPLUS_STORAGE_OBJECT_PATH_STYLE_ACCESS_ENABLED=<true-or-false>
```

Common first-deploy failures:

- `SPRING_DATASOURCE_URL is required`: confirm `DATABASE_URL` is present on the
  web service, or set the Spring datasource values manually.
- `Prod profile must not use an H2 datasource`: replace the local H2 URL.
- `replus.web-base-url is required`: set `REPLUS_WEB_BASE_URL` to the HTTPS
  frontend origin used for invite links.
- `Prod profile requires an HTTPS replus.web-base-url`: replace local or HTTP
  frontend URLs with the HTTPS frontend origin.
- `replus.storage.object-storage.public-base-url is required`: set
  `REPLUS_STORAGE_OBJECT_PUBLIC_BASE_URL`, or temporarily set
  `REPLUS_STORAGE_MODE=local` for a backend-only smoke deploy.
- `Prod profile requires an HTTPS replus.storage.object-storage.public-base-url`:
  replace local or HTTP playback URLs with the HTTPS playback base URL.
- `replus.web.cors.allowed-origins is required`: set explicit frontend origins.
- `Prod profile must not use localhost CORS origins`: remove local frontend
  origins from `REPLUS_WEB_CORS_ALLOWED_ORIGINS`.
- `Prod profile requires HTTPS CORS origins`: replace HTTP frontend origins
  with HTTPS frontend origins.
- Readiness is down with a `db` component failure: check database network access
  and credentials.
- Readiness is up but upload flows fail: switch from local smoke storage to a
  configured object storage provider.

## Required Production Variables

Set the production profile explicitly:

```text
SPRING_PROFILES_ACTIVE=prod
```

Required for production startup:

```text
DATABASE_URL=<postgresql-connection-string>
REPLUS_WEB_BASE_URL=<https-frontend-origin>
REPLUS_WEB_CORS_ALLOWED_ORIGINS=<https-frontend-origin>
```

Guest sessions are disabled by default in the `prod` profile. Enable them only
for an explicit validation window until the production social-login flow is in
place:

```text
REPLUS_AUTH_GUEST_SESSION_ENABLED=true
```

For production social login, configure the Google and Apple OIDC audiences that
the backend should accept. Values are comma-separated because one provider can
issue tokens to multiple app clients, such as web and native clients:

```text
REPLUS_AUTH_GOOGLE_CLIENT_IDS=<google-web-client-id>,<google-ios-client-id>
REPLUS_AUTH_APPLE_CLIENT_IDS=<apple-service-id-or-bundle-id>
```

Detailed provider setup and smoke validation steps live in
[social-login-setup.md](social-login-setup.md).

If these values are omitted, `/api/auth/social` still deploys but fails closed:
all provider tokens are rejected with `401 UNAUTHENTICATED`.

Production sessions are database-backed by default. Keep the explicit value in
the hosting dashboard so a deployment cannot accidentally fall back to in-memory
sessions:

```text
REPLUS_AUTH_SESSION_STORE=database
```

`DATABASE_URL` is the preferred Render path. Non-Render deployments can set the
Spring datasource values directly instead:

```text
SPRING_DATASOURCE_URL=<postgresql-jdbc-url>
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<database-password>
```

The `prod` profile fails fast when:

- `SPRING_DATASOURCE_URL` is blank.
- The datasource URL points to H2.
- The auth session store is not `database`.
- The web base URL is blank, points to localhost, or is not HTTPS.
- Object storage mode is enabled while the public playback base URL is blank,
  points to localhost, or is not HTTPS.
- Development seed data is enabled.
- Fixed development bearer tokens are enabled.
- The H2 console is enabled.
- CORS origins are blank.
- CORS origins include a wildcard.
- CORS origins point to localhost or use HTTP instead of HTTPS.

## Storage Variables

Local storage mode is useful for development only. Production video upload
flows should use object storage:

```text
REPLUS_STORAGE_MODE=object-storage
REPLUS_STORAGE_OBJECT_BUCKET=<bucket-name>
REPLUS_STORAGE_OBJECT_PUBLIC_BASE_URL=<public-playback-base-url>
REPLUS_STORAGE_OBJECT_REGION=<region-or-provider-region>
REPLUS_STORAGE_OBJECT_ENDPOINT=<provider-endpoint-if-required>
REPLUS_STORAGE_OBJECT_PATH_STYLE_ACCESS_ENABLED=<true-or-false>
```

Credential variables for object storage are provider-specific. Keep them in the
hosting platform secret manager and never commit them to the repository.

## Optional Variables

```text
PORT=<platform-port>
REPLUS_WEB_BASE_URL=<frontend-base-url>
REPLUS_MISSION_LIFECYCLE_ZONE=Asia/Seoul
REPLUS_MISSION_LIFECYCLE_SCHEDULER_ENABLED=false
REPLUS_MISSION_LIFECYCLE_FAIL_INCOMPLETE_CRON=0 5 0 * * *
REPLUS_MISSION_LIFECYCLE_RELEASE_DUE_FIXED_DELAY=30000
REPLUS_MISSION_LIFECYCLE_RELEASE_DUE_INITIAL_DELAY=30000
```

The mission lifecycle scheduler is disabled by default. Enable it only after the
release/failure worker behavior has been reviewed for the target environment.

## Development-Only Defaults

These defaults are for local development and tests:

```text
H2_CONSOLE_ENABLED=true
REPLUS_SEED_DEV_DATA=true
REPLUS_AUTH_DEV_FIXED_TOKENS_ENABLED=true
REPLUS_AUTH_SESSION_STORE=database
SPRING_DATASOURCE_URL=jdbc:h2:mem:...
REPLUS_STORAGE_MODE=local
```

Do not use them for a production deployment.

## Pre-Deploy Checklist

- CI is passing on the commit being deployed.
- Render auto-deploy is set to wait for GitHub checks to pass.
- `SPRING_PROFILES_ACTIVE=prod` is set.
- `DATABASE_URL` is populated from the Render PostgreSQL database, or explicit
  Spring datasource credentials are configured in the secret manager.
- Fixed development bearer tokens are disabled.
- CORS origins contain only explicit HTTPS frontend origins.
- `REPLUS_STORAGE_MODE` is set explicitly in the hosting dashboard.
- Object storage bucket, region, endpoint, and playback base URL are configured
  before validating upload flows.
- No `.env` file, private key, token, or real endpoint has been committed.
- Database migrations have been reviewed for backward compatibility.
- The release worker remains disabled unless it is intentionally being operated.

## Post-Deploy Smoke Checks

The repository includes a smoke script for the first deployed URL:

```bash
scripts/smoke-api.sh https://<api-host>
```

To also validate the guest session and `/api/me` flow:

```bash
scripts/smoke-api.sh --with-guest-auth https://<api-host>
```

The guest-auth check requires `REPLUS_AUTH_GUEST_SESSION_ENABLED=true` in the
deployment environment. Leave it disabled for normal production operation until
the production social-login flow is available.

To validate that the deployed social-login endpoint exists and rejects invalid
tokens without needing real Google or Apple credentials:

```bash
scripts/smoke-api.sh --with-social-auth-failure https://<api-host>
```

This check is safe to run in normal production because it does not create users
or sessions.

After Google or Apple client IDs are configured, validate the full social-login
session flow with a short-lived test id token. Do not commit or paste the token
into issue comments, PRs, or logs:

```bash
export SMOKE_SOCIAL_AUTH_PROVIDER=GOOGLE
read -r -s SMOKE_SOCIAL_AUTH_TOKEN
export SMOKE_SOCIAL_AUTH_TOKEN
scripts/smoke-api.sh --with-social-auth-success https://<api-host>
unset SMOKE_SOCIAL_AUTH_TOKEN
```

Use `SMOKE_SOCIAL_AUTH_PROVIDER=APPLE` when validating an Apple id token. The
provider value is case-insensitive. The success smoke creates or reuses the
social-login user and then verifies `/api/me` with the issued backend bearer
session.

For Render cold starts or immediately after a deploy, allow a longer request
timeout and a few retries:

```bash
SMOKE_CURL_TIMEOUT_SECONDS=180 \
SMOKE_RETRY_ATTEMPTS=6 \
SMOKE_RETRY_DELAY_SECONDS=10 \
scripts/smoke-api.sh --with-social-auth-failure https://<api-host>
```

The guest-auth smoke creates a guest user, so reserve it for a smoke
environment or an explicit production validation window.

After deployment, check:

```bash
curl -fsS https://<api-host>/actuator/health/liveness
curl -fsS https://<api-host>/actuator/health/readiness
curl -fsS https://<api-host>/actuator/info
```

Expected result:

- Liveness status is `UP`.
- Readiness status is `UP`.
- Readiness includes `db` and `storage` components.
- Info includes the public app metadata, build version, and git commit id.
- Info includes `auth.social.providers.*.clientIdsConfigured` booleans so the
  deployed Google and Apple login configuration can be checked without exposing
  the actual client IDs.

If readiness is down, inspect the platform logs first. The production guard is
designed to fail early with clear messages for unsafe configuration.

## Rollback Notes

- Prefer redeploying the previous known-good artifact or image.
- Do not roll back database migrations blindly.
- Keep schema changes backward-compatible until the previous application version
  is no longer needed.
- If object storage configuration changes caused the incident, restore the
  previous bucket, endpoint, and public playback base URL values from the secret
  manager history.
