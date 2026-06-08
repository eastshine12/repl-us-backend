# repl.us Backend

**repl.us** is a private social room for close friends who want a small daily ritual together. Each room gets one prompt a day, and every active member answers with a fixed 3-second silent-friendly video clip. Friend responses stay hidden until everyone has joined in, then the room opens the shared moment together.

The product is designed around intimacy rather than broadcast. Rooms are small, invite-only, and built for 3-6 friends. The backend keeps the daily mission, membership, invite, response visibility, and release policies consistent so the client can focus on capture, playback, and the room experience.

## Features

- Small private rooms with active membership checks.
- Daily room mission lookup with on-demand mission creation.
- Owner-only mission editing before the first active response.
- Opaque invite codes that do not expose room IDs.
- Authenticated invite join flow with a 6-member room limit.
- Participation state for today's mission, including submitted counts and release readiness.
- Direct response-video upload flow with object-key reservation and object-storage verification.
- Operational health and readiness probes for deployment environments.
- Foundation storage model for users, rooms, members, invites, missions, responses, video assets, and mission release state.
- Development bearer-session flow for local testing before social login integration.

This backend is being built in small production slices: room access, invites, daily mission policy, response upload confirmation, and social interactions are added incrementally while release workers, push notifications, and native-app integration remain separate steps.

## Architecture

The backend follows a traditional DDD layered structure while keeping Clean Architecture dependency direction:

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

Domain models and policies stay framework-free. Application facades own transaction boundaries and use-case orchestration. Infrastructure implements domain repository contracts with Spring Data JPA. REST controllers handle authentication extraction, request validation, and DTO mapping.

## Tech Stack

- Kotlin 2.2
- Java 21
- Spring Boot 4.0
- Spring Web MVC
- Spring Data JPA
- Flyway
- H2 for local development and tests
- PostgreSQL driver for deployment targets
- AWS SDK for Java v2 for S3-compatible object storage integration
- JUnit 5 and AssertJ

## Local Run

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080` by default. Local seed data is enabled for development:

```text
Authorization: Bearer dev-token-mina
Seed room: aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa
Seed invite code: R3S9KQ
```

Example:

```bash
curl -H "Authorization: Bearer dev-token-mina" http://localhost:8080/api/me
```

Readiness probe:

```bash
curl http://localhost:8080/actuator/health/readiness
```

You can also create a temporary guest session:

```bash
curl -X POST http://localhost:8080/api/auth/guest \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Mina"}'
```

## Tests

```bash
./gradlew test
```

Policy tests cover active room membership, owner-only mission edits, edit lock after the first active response, room capacity, and opaque invite code generation.

## API Documentation

The current API contract lives in:

```text
docs/api/openapi.yaml
```

Keep this contract in sync with client integration work.
