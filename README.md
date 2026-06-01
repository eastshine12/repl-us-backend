# repl-us-backend

Kotlin Spring Boot backend for **repl.us**, a private social MVP where close friends answer one daily prompt with fixed 3-second video responses. This repository is designed to be safe for a public portfolio backend: no private frontend code, production secrets, real service endpoints, private keys, or checked-in `.env` values are required.

## Core Features

- Development bearer-session stub for local API work before real social login.
- Current user and active room list lookup.
- Room detail lookup guarded by active room membership.
- Daily room mission lookup with on-demand creation.
- Invite link creation with opaque invite codes.
- Authenticated invite join flow with a 6-member room limit.
- Owner-only mission editing before the first active response.
- Foundation domain model for users, rooms, members, invite links, missions, responses, video assets, and mission release state.

The first slice intentionally excludes object storage uploads, comments/reactions APIs, the release timer worker, push notifications, and native-app integration.

## Architecture

The app uses a traditional DDD layered structure while keeping Clean Architecture dependency direction:

```text
interfaces -> application -> domain
infrastructure -> domain contracts
```

Packages are split by domain first, then by layer:

```text
com.replus.api
  auth/
  room/
  mission/
  common/
```

Domain models and policies stay framework-free. Application facades own transaction boundaries and use-case orchestration. Infrastructure implements domain repository contracts with Spring Data JPA. REST controllers only handle authentication extraction, request validation, and DTO mapping.

## Tech Stack

- Kotlin 2.2
- Java 21
- Spring Boot 4.0
- Spring Web MVC
- Spring Data JPA
- Flyway
- H2 for local development and tests
- PostgreSQL driver for production-style deployment targets
- JUnit 5 and AssertJ

## Local Run

```bash
cd apps/api
./gradlew bootRun
```

The app starts on `http://localhost:8080` by default. Local dev seed data is enabled by default:

```text
Authorization: Bearer dev-token-mina
Seed room: aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa
Seed invite code: R3S9KQ
```

Example:

```bash
curl -H "Authorization: Bearer dev-token-mina" http://localhost:8080/api/me
```

You can also create a temporary guest session:

```bash
curl -X POST http://localhost:8080/api/auth/guest \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Mina"}'
```

## Tests

```bash
cd apps/api
./gradlew test
```

Policy tests cover active room membership, owner-only mission edits, edit lock after the first active response, room capacity, and opaque invite code generation.

## Environment Variables

See [.env.example](./.env.example).

```text
PORT=8080
SPRING_DATASOURCE_URL=jdbc:h2:mem:replus;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=
SPRING_DATASOURCE_DRIVER=org.h2.Driver
REPLUS_WEB_BASE_URL=http://localhost:3000
REPLUS_SEED_DEV_DATA=true
H2_CONSOLE_ENABLED=true
```

For PostgreSQL, set the datasource URL, username, password, and driver through the runtime environment. Do not commit real credentials.

## API Documentation

The current API contract lives in:

```text
docs/api/openapi.yaml
```

Keep this contract in sync with the mobile/web client integration work.
