# Event Alerting Platform

Public self-serve platform where users define alert rules for news, market
moves, and natural disasters, and get notified over email/Slack (more
channels later).

**Status:** M0 (foundation), M1 (auth), M2 (alert rules & channels), M3
(notifications) complete.

## Stack
- Java 17
- Spring Boot 3.2.5 — Web, JDBC (`NamedParameterJdbcTemplate`, no JPA/Hibernate), Security, Validation, Mail, Actuator
- PostgreSQL 16
- Flyway
- JJWT for token issuing/parsing
- MailHog (dev-only fake SMTP server + web UI)
- Docker / Docker Compose

## Architecture: layered (Model / View / Controller / Service / Repository)

Every layer lives in its own top-level package under `com.eventalert`.
`security` and `exception` sit alongside the five named layers as
cross-cutting concerns rather than being forced into one of them.

```
src/main/java/com/eventalert/
  EventAlertingApplication.java

  model/        domain classes + request payloads
    User, Role, RegisterRequest, LoginRequest, AuthResponse
    Category, ChannelType, AlertRule, Channel, AlertRuleRequest, ChannelRequest
    Delivery, DeliveryStatus, NotificationMessage, TestNotificationRequest

  view/         what actually leaves the API — never the model classes directly
    UserView              hides passwordHash entirely (not just @JsonIgnore)
    AlertRuleView
    ChannelView            masks the Slack webhook URL in config
    DeliveryView

  controller/   REST endpoints
    AuthController, AlertRuleController, ChannelController
    GlobalExceptionHandler

  service/      business logic
    AuthService, AlertRuleService, ChannelService, DeliveryService
    CriteriaValidator (+ News/Market/Disaster impls) + CriteriaValidatorDispatcher
    ChannelConfigValidator (+ Email/Slack impls) + ChannelConfigValidatorDispatcher
    NotificationChannel (+ Email/Slack impls) + NotificationChannelDispatcher

  repository/   data access — JdbcTemplate only, no Spring Data JPA
    UserRepository, AlertRuleRepository, ChannelRepository, DeliveryRepository

  security/     JWT + Spring Security wiring
    JwtService, JwtAuthenticationFilter, SecurityConfig,
    CustomUserDetailsService, CurrentUserService

  exception/    domain exceptions, mapped to HTTP responses by GlobalExceptionHandler
    EmailAlreadyExistsException, InvalidCredentialsException,
    InvalidCriteriaException, InvalidChannelConfigException,
    AlertRuleNotFoundException, ChannelNotFoundException,
    NotificationDeliveryException, NoChannelsLinkedException

src/main/resources/
  application.yml                            config (datasource, JWT secret, mail)
  db/migration/V1__init_schema.sql            full v1 schema
  db/migration/V2__deliveries_event_id_nullable.sql   see note below

postman/
  event-alerting-platform.postman_collection.json   importable collection, see below
```

### Why a view layer instead of `@JsonIgnore`

Controllers never return `model` objects directly — they return `view`
records, and those records simply don't have a field for anything sensitive.
For `User`, that means no `passwordHash` field on `UserView` at all. For
`Channel`, it goes further: the Slack `webhookUrl` inside the generic
`config` map is a bearer credential (anyone holding it can post to the
channel), so `ChannelView` masks it before it's serialized, which
`@JsonIgnore` on a top-level field could never do for something buried
inside a `Map`.

### Why `deliveries.event_id` is nullable (V2 migration)

Real event ingestion is M4, not built yet. So M3 ships a manual
**"send test notification"** endpoint (see below) that exercises the full
send path — validation, dispatch, retry, delivery logging — without a real
ingested event behind it. Those test sends have no `event_id`, so the
original `NOT NULL` constraint from V1 had to relax. Once M4 lands,
ingestion-triggered deliveries will still always set `event_id`; this only
opened the door for the manual-test path.

## Run everything with Docker
```bash
docker compose up --build
```
- App: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Postgres: localhost:5432 (db/user/pass default to `eventalert` — see `.env.example`)
- MailHog web UI: http://localhost:8025 — every email the app sends shows up here instead of a real inbox

## Run from IntelliJ (recommended while developing)
1. Open this folder in IntelliJ as a Maven project (it'll auto-import).
2. Start the database and mail server: `docker compose up -d postgres mailhog`
3. Run `EventAlertingApplication` from IntelliJ — it connects to
   `localhost:5432` and `localhost:1025` (mail) by default (see `application.yml`).
4. Flyway applies both migrations automatically on startup; check the
   console log for confirmation.

To use custom DB/mail settings, copy `.env.example` to `.env` and adjust —
`docker compose` picks it up automatically, and you can export the same
variables in your IntelliJ run configuration for local runs.

## Testing the API with Postman

Import `postman/event-alerting-platform.postman_collection.json` into
Postman (File → Import, or drag the file in). It's a self-contained
collection — no separate environment file needed, everything it needs is a
collection variable with a default already set.

**Collection variables** (Collection → Variables tab if you want to see/edit them):
| Variable | Purpose |
|---|---|
| `base_url` | Defaults to `http://localhost:8080` |
| `token` | Set automatically by **Auth → Login**'s test script |
| `channel_id` | Set automatically by **Channels → Create Channel - Slack** |
| `alert_rule_id` | Set automatically by **Alert Rules → Create Alert Rule - Disaster** |
| `news_rule_id` | Set automatically by **Alert Rules → Create Alert Rule - News** (has no channels linked — used to test the "no channels" error) |

**Suggested run order** (each request has test-tab assertions that show
pass/fail in Postman's Test Results):
1. `Auth → Register` then `Auth → Login` — Login saves `{{token}}`, which
   every other request already has wired up via Bearer auth, nothing to
   copy/paste manually.
2. `Channels → Create Channel - Email` and `Create Channel - Slack` — the
   Slack request saves `{{channel_id}}`. Note: the collection ships with a
   placeholder webhook URL, so `verified` will come back `false` unless you
   swap in a real one from your own Slack app.
3. `Alert Rules → Create Alert Rule - News / Market / Disaster` — the
   Disaster request uses `{{channel_id}}` and saves `{{alert_rule_id}}`, then
   `List / Get / Update / Delete` all use it.
4. `Notifications → Send Test Notification` — sends through every channel
   linked to `{{alert_rule_id}}` (the Disaster rule) and returns one
   delivery-log entry per channel. With `docker compose up` running, the
   EMAIL delivery should come back `SENT` — check
   [http://localhost:8025](http://localhost:8025) to see it. The SLACK
   delivery will come back `FAILED` against the placeholder webhook URL,
   which is expected.
5. `Validation examples (expected to fail)` — deliberately malformed
   requests (missing keywords, out-of-range magnitude, bad webhook URL,
   duplicate email, wrong password, no token, notifying a rule with no
   channels linked) that should each return the 4xx status asserted in
   their test script.

Run the whole thing unattended with **Runner** (top toolbar) or
`newman run postman/event-alerting-platform.postman_collection.json` if you
have Newman installed — the folder order above is also the collection's
natural top-to-bottom order.

## Alert rule criteria shapes (hand-rolled per category)

```jsonc
// NEWS
{ "keywords": ["interest rate", "recession"], "match": "any" }  // match: "any" | "all", optional

// MARKET
{ "symbol": "AAPL", "changePercent": { "gte": 5 } }              // gte and/or lte, at least one required

// DISASTER
{ "minMagnitude": 6.0, "region": { "lat": 37.77, "lon": -122.41, "radiusKm": 500 } }  // region optional
```
Anything that doesn't match these shapes is rejected with a 400 before it
reaches the database — see `service/NewsCriteriaValidator.java`,
`MarketCriteriaValidator.java`, `DisasterCriteriaValidator.java`.

## Sending a notification (M3)

`POST /api/alert-rules/{id}/test-notification` with `{"title": "...", "body": "..."}`
sends that message through every channel linked to the rule and returns one
`DeliveryView` per channel. Each send is retried up to 3 times (linear
backoff) before being logged as `FAILED`; a `deliveries` row is written
exactly once per channel, with the final outcome. This is a manual trigger
standing in for M4's real event-driven dispatch — once ingestion exists, the
matching engine calls the same `NotificationChannel` / retry / logging code
this endpoint already exercises.

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
- **M2** — Alert rule + channel CRUD, hand-rolled per-category criteria validation, Slack webhook verification ping on channel creation, dedicated view layer, Postman collection
- **M3** — `NotificationChannel` abstraction (Email via MailHog, Slack via webhook), retrying delivery dispatch, delivery logging, manual test-notification endpoint

## Next: M4 — Event Ingestion
`EventSource` interface + scheduler; connectors for news, market, and
disaster data; normalization into the `events` table; the matching engine
that turns a real ingested event into calls to the same delivery path M3
built.
