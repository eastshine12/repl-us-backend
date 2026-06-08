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
service. It uses the Dockerfile, points the health check at
`/actuator/health/readiness`, and keeps secrets out of git with `sync: false`
environment variables. The blueprint starts on the free web service plan to
avoid surprise spend during the first smoke deployment. Review the plan before
serving real users.

Before applying the blueprint, prepare:

- A PostgreSQL database.
- A JDBC-formatted datasource URL for Spring, such as
  `<postgresql-jdbc-url>`.
- Explicit HTTPS frontend origins for `REPLUS_WEB_CORS_ALLOWED_ORIGINS`.
- Object storage values for response-video uploads.

Render PostgreSQL connection strings are commonly provided as
`postgresql://...`. The Spring datasource URL must be JDBC-formatted:
`jdbc:postgresql://...`. Convert the value before setting
`SPRING_DATASOURCE_URL`.

After the blueprint creates the service, set every `sync: false` value in the
Render Dashboard before expecting the service to become healthy. Missing values
are intentionally caught by the production guard during startup.

## Required Production Variables

Set the production profile explicitly:

```text
SPRING_PROFILES_ACTIVE=prod
```

Required for production startup:

```text
SPRING_DATASOURCE_URL=<postgresql-jdbc-url>
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<database-password>
REPLUS_WEB_CORS_ALLOWED_ORIGINS=<https-frontend-origin>
```

The `prod` profile fails fast when:

- `SPRING_DATASOURCE_URL` is blank.
- The datasource URL points to H2.
- Development seed data is enabled.
- The H2 console is enabled.
- CORS origins are blank.
- CORS origins include a wildcard.

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
SPRING_DATASOURCE_URL=jdbc:h2:mem:...
REPLUS_STORAGE_MODE=local
```

Do not use them for a production deployment.

## Pre-Deploy Checklist

- CI is passing on the commit being deployed.
- `SPRING_PROFILES_ACTIVE=prod` is set.
- PostgreSQL credentials are configured in the secret manager.
- CORS origins contain only explicit HTTPS frontend origins.
- Object storage bucket, region, endpoint, and playback base URL are configured.
- No `.env` file, private key, token, or real endpoint has been committed.
- Database migrations have been reviewed for backward compatibility.
- The release worker remains disabled unless it is intentionally being operated.

## Post-Deploy Smoke Checks

After deployment, check:

```bash
curl -fsS https://<api-host>/actuator/health/liveness
curl -fsS https://<api-host>/actuator/health/readiness
```

Expected result:

- Liveness status is `UP`.
- Readiness status is `UP`.
- Readiness includes `db` and `storage` components.

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
