# Event Alerting Platform — M0: Project Foundation

Public self-serve platform where users define alert rules for news, market
moves, and natural disasters, and get notified over email/Slack (more
channels later). This is the **M0** milestone: a runnable, Dockerized Spring
Boot skeleton wired to Postgres via Flyway. No business logic yet — that
starts at M1 (auth).

## Stack
- Java 17
- Spring Boot 3.2.5 (Web, Data JPA, Validation, Actuator)
- PostgreSQL 16
- Flyway
- Docker / Docker Compose

## Project structure
```
src/main/java/com/eventalert/
  EventAlertingApplication.java     entry point
src/main/resources/
  application.yml                   config (env-driven datasource)
  db/migration/V1__init_schema.sql  full v1 schema: users, channels,
                                     alert_rules, alert_rule_channels,
                                     events, deliveries
src/test/java/com/eventalert/
  EventAlertingApplicationTests.java  context-loads smoke test
Dockerfile
docker-compose.yml
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

## Running tests
`EventAlertingApplicationTests` boots the full Spring context, which needs a
reachable Postgres — run `docker compose up -d postgres` first, then:
```bash
mvn test
```
(Testcontainers-based tests that don't need an external DB running arrive in
M7 — hardening.)

## What's in this milestone
- Runnable Spring Boot skeleton, no business logic yet
- Full v1 schema applied via Flyway baseline migration
- Docker Compose for app + Postgres
- Actuator health endpoint

## Next: M1 — Auth & Users
Registration/login, JWT, `USER`/`ADMIN` roles.
