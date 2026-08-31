# Event Alerting Platform

Public self-serve platform where users define alert rules for news, market
moves, and natural disasters, and get notified over email/Slack (more
channels later).

**Status:** M0 (foundation) and M1 (auth) complete.

## Stack
- Java 17
- Spring Boot 3.2.5 — Web, JDBC (`NamedParameterJdbcTemplate`, no JPA/Hibernate), Security, Validation, Actuator
- PostgreSQL 16
- Flyway
- JJWT for token issuing/parsing
- Docker / Docker Compose

## Architecture: layered (Model / Controller / Service / Repository)

Every layer lives in its own top-level package under `com.eventalert`. There's
no dedicated `view` directory — controllers return `model` objects directly
(sensitive fields like `passwordHash` are hidden via `@JsonIgnore` instead of
a separate DTO layer). `security` and `exception` sit alongside the five
named layers as cross-cutting concerns rather than being forced into one of
them.

```
src/main/java/com/eventalert/
  EventAlertingApplication.java

  model/            domain classes + request/response payloads
    User.java, Role.java
    RegisterRequest.java, LoginRequest.java, AuthResponse.java

  controller/       REST endpoints
    AuthController.java
    GlobalExceptionHandler.java

  service/          business logic
    AuthService.java

  repository/        data access - JdbcTemplate only, no Spring Data JPA
    UserRepository.java

  security/          JWT + Spring Security wiring
    JwtService.java, JwtAuthenticationFilter.java,
    SecurityConfig.java, CustomUserDetailsService.java

  exception/          domain exceptions, mapped to HTTP responses by
                       GlobalExceptionHandler
    EmailAlreadyExistsException.java, InvalidCredentialsException.java

src/main/resources/
  application.yml                   config (env-driven datasource, JWT secret)
  db/migration/V1__init_schema.sql  full v1 schema
```

## Run everything with Docker
```bash
docker compose up --build
```
- App: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Postgres: localhost:5432 (db/user/pass default to `eventalert` — see `.env.example`)

## Run from IntelliJ (recommended while developing)
1. Open this folder in IntelliJ as a Maven project (it'll auto-import).
2. Start just the database: `docker compose up -d postgres`
3. Run `EventAlertingApplication` from IntelliJ — it connects to
   `localhost:5432` by default (see `application.yml`).
4. Flyway applies `V1__init_schema.sql` automatically on startup; check the
   console log for confirmation.

To use custom DB credentials, copy `.env.example` to `.env` and adjust —
`docker compose` picks it up automatically, and you can export the same
variables in your IntelliJ run configuration for local runs.

## Auth endpoints (M1)

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"correct-horse-battery"}'

# Login -> returns a Bearer token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"correct-horse-battery"}'

# Use the token on a protected endpoint (none exist yet beyond /api/auth/**
# and /actuator/health, which are public - this becomes relevant from M2 on)
curl http://localhost:8080/api/some-protected-endpoint \
  -H "Authorization: Bearer <token>"
```

`security.jwt.secret` in `application.yml` has a dev-only default — override
it with the `JWT_SECRET` env var anywhere real. Token lifetime is controlled
by `JWT_EXPIRATION_MS` (default 24h).

## Running tests
`EventAlertingApplicationTests` boots the full Spring context, which needs a
reachable Postgres — run `docker compose up -d postgres` first, then:
```bash
mvn test
```
(Testcontainers-based tests that don't need an external DB running arrive in
M7 — hardening.)

## What's in place so far
- **M0** — Runnable Spring Boot skeleton, Docker Compose, full v1 schema via Flyway, Actuator health endpoint
- **M1** — Registration/login, JWT issuing + validation, `USER`/`ADMIN` roles, stateless Spring Security, layered package structure, JDBC-only data access

## Next: M2 — Alert Rules & Channels
CRUD APIs, per-category criteria validation, channel setup (email default,
Slack webhook + verification ping).
