# Event Alerting Platform

Public self-serve platform where users define alert rules for news, market
moves, and natural disasters, and get notified over email/Slack (more
channels later).

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
  EventAlertingApplication.java   @EnableScheduling for the ingestion poller

  model/        domain classes + request payloads
    User, Role, RegisterRequest, LoginRequest, AuthResponse
    Category, ChannelType, AlertRule, Channel, AlertRuleRequest, ChannelRequest
    Delivery, DeliveryStatus, NotificationMessage, TestNotificationRequest
    Event, RawEvent

  view/         what actually leaves the API — never the model classes directly
    UserView               hides passwordHash entirely (not just @JsonIgnore)
    AlertRuleView                                  ChannelView (masks the Slack webhook URL)
    DeliveryView
    AdminAlertRuleView, AdminDeliveryView, EventView, SourceStatusView   admin-only —
      include cross-user fields (owner email/id) the per-user views deliberately omit

  controller/   REST endpoints
    AuthController, AlertRuleController, ChannelController, AdminController
    GlobalExceptionHandler

  service/      business logic
    AuthService, AlertRuleService, ChannelService, DeliveryService, AdminService
    CriteriaValidator (+ News/Market/Disaster impls) + CriteriaValidatorDispatcher
    ChannelConfigValidator (+ Email/Slack impls) + ChannelConfigValidatorDispatcher
    NotificationChannel (+ Email/Slack impls) + NotificationChannelDispatcher
    EventSource (+ News/Market/Disaster impls)
    EventMatcher (+ News/Market/Disaster impls) + EventMatcherDispatcher
    MatchingService, IngestionScheduler

  repository/   data access — JdbcTemplate only, no Spring Data JPA
    UserRepository, AlertRuleRepository, ChannelRepository, DeliveryRepository, EventRepository

  security/     JWT + Spring Security wiring
    JwtService, JwtAuthenticationFilter, SecurityConfig,
    CustomUserDetailsService, CurrentUserService, RateLimitingFilter

  config/       cross-cutting Spring config outside the five named layers
    AsyncConfig   dedicated thread pool for background delivery dispatch

  exception/    domain exceptions, mapped to HTTP responses by GlobalExceptionHandler
    EmailAlreadyExistsException, InvalidCredentialsException,
    InvalidCriteriaException, InvalidChannelConfigException,
    AlertRuleNotFoundException, ChannelNotFoundException,
    NotificationDeliveryException, NoChannelsLinkedException

src/main/resources/
  application.yml                            config (datasource, JWT secret, mail, ingestion, rate limits)
  application-prod.yml                       prod profile — no default secrets, fails fast if unset
  db/migration/V1__init_schema.sql            full v1 schema
  db/migration/V2__deliveries_event_id_nullable.sql

src/test/java/com/eventalert/
  AbstractIntegrationTest.java   Testcontainers Postgres base class — no manual DB setup for `mvn test`
  EventAlertingApplicationTests.java
  controller/AuthIntegrationTest.java, AlertRuleIntegrationTest.java

postman/
  event-alerting-platform.postman_collection.json   importable collection, see below
```

## Run everything with Docker
```bash
docker compose up --build
```
- App: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Postgres: localhost:5432 (db/user/pass default to `eventalert` — see `.env.example`)
- MailHog web UI: http://localhost:8025 — every email the app sends shows up here

## Run from IntelliJ (recommended while developing)
1. Open this folder in IntelliJ as a Maven project (it'll auto-import).
2. Start the database and mail server: `docker compose up -d postgres mailhog`
3. Run `EventAlertingApplication` from IntelliJ — it connects to
   `localhost:5432` and `localhost:1025` (mail) by default (see `application.yml`).
4. Flyway applies both migrations automatically on startup; check the
   console log for confirmation.

To use custom DB/mail/ingestion settings, copy `.env.example` to `.env` and
adjust — `docker compose` picks it up automatically, and you can export the
same variables in your IntelliJ run configuration for local runs.

## Event ingestion

Three `EventSource` implementations, one per category, polled on a schedule
(`INGESTION_POLL_INTERVAL_MS`, default 2 minutes) and deduped on
`(source, external_id)` before the matching engine ever sees them:

| Category | Source | API key needed? |
|---|---|---|
| DISASTER | USGS real-time earthquake feed | **No** — works out of the box |
| NEWS | NewsAPI.org `/v2/top-headlines` | `NEWSAPI_KEY` |
| MARKET | Finnhub `/quote` (polls a symbol watchlist — `FINNHUB_SYMBOLS`) | `FINNHUB_KEY` |

NEWS and MARKET simply stay idle (return no events, log nothing alarming) if
their key isn't set — the app runs fine without them. DISASTER needs
nothing and is the easiest one to see work end to end.

**How a match actually happens:** each poll cycle, every newly-inserted
event is handed to `MatchingService`, which pulls every *active* alert rule
in that event's category and runs it through the matching category's
`EventMatcher` (mirrors the `CriteriaValidator` used at rule-creation time,
but checking an event instead of validating a shape). A match calls the same
`DeliveryService` retry/logging path M3 built — so a real ingested match and
a manual test-notification produce identical `deliveries` rows, just with
`event_id` set on the real one.

**Testing it without waiting ~2 minutes for the scheduler:**
`POST /api/admin/ingestion/poll-now` triggers a poll immediately and returns
how many new events each source produced. It's gated by `ROLE_ADMIN` like
the rest of `/api/admin/**` — register with `"isAdmin": true` to get one:
```json
{"email": "admin@example.com", "password": "correct-horse-battery", "isAdmin": true}
```
**Self-service admin promotion at registration — anyone who can call
`/api/auth/register` can set `isAdmin: true` and get `ROLE_ADMIN`
immediately.** That's a deliberate simplification for this stage of the
project, not an oversight: fine while only trusted people can reach
registration, not fine the moment this is public-facing. If that changes,
this needs to go behind an invite/allowlist, an approval step, or be
removed from self-registration entirely in favor of an admin-only
user-management endpoint.

**To actually see a match fire:** create a DISASTER rule with a low
threshold — magnitude 1+ earthquakes happen constantly, so
`{"minMagnitude": 1.0}` (no `region`) against a channel you've linked will
very likely produce a real `SENT`/`FAILED` delivery on the next poll. Check
`GET /api/alert-rules/{id}/deliveries` afterward.

## Admin API

Read-only, `ROLE_ADMIN`-gated visibility across every user — this is the
actual admin view the original brief asked for; `/api/admin/ingestion/poll-now`
(M4) was an operational trigger, not this.

| Endpoint | Returns |
|---|---|
| `GET /api/admin/users` | Every registered user (same shape as `UserView` — no password hash) |
| `GET /api/admin/alert-rules` | Every alert rule, across every user, with the owner's email attached |
| `GET /api/admin/events?category=&since=` | Ingested events, both filters optional, capped at 200 most recent |
| `GET /api/admin/deliveries?status=&since=` | Every delivery, across every user, with the owning `userId` attached, both filters optional, capped at 200 most recent |
| `GET /api/admin/sources/status` | Per-`EventSource` health: whether it's configured (has an API key, or needs none), when it last polled, how many events it last produced, its last error if any |

No promotion endpoint exists — register with `"isAdmin": true` (see
"Event ingestion" above for the tradeoff that implies). `category`/`status`
query params match the enum names exactly (`NEWS`, `MARKET`, `DISASTER` /
`SENT`, `FAILED`, `PENDING`); `since` takes an ISO-8601 timestamp, e.g.
`2026-08-01T00:00:00Z`.

## Hardening

**Observability.** `/actuator/metrics` and `/actuator/prometheus` are now
exposed (in addition to `health`/`info`), gated by `ROLE_ADMIN` — health and
info stay public for things like Docker healthchecks and uptime monitors
that shouldn't need a token. Two custom counters were added on top of the
usual JVM/HTTP metrics Actuator gives you for free:
- `ingestion.events.ingested{source=...}` — incremented per source, per poll
- `deliveries.total{channel=...,status=...}` — incremented per delivery attempt

**API docs.** Swagger UI at `/swagger-ui.html`, raw OpenAPI JSON at
`/v3/api-docs` — both public, generated automatically from the existing
controllers with no extra annotations needed.

**Rate limiting.** `RateLimitingFilter` — a simple in-memory, fixed-window
limiter (~10 req/min on `/api/auth/**`, ~120 req/min elsewhere by default,
both configurable). It keys by authenticated user where possible, falling
back to IP for anonymous requests like login/register. **This is
single-instance only** — the counters live in memory and reset on restart,
so it stops being sufficient the moment this runs as more than one
instance; a shared store (Redis, etc.) would be the fix then, not now.

**Integration tests.** `mvn test` now spins up a real, ephemeral Postgres
via Testcontainers automatically — no more `docker compose up -d postgres`
before running tests. `AuthIntegrationTest` and `AlertRuleIntegrationTest`
exercise the full stack (real HTTP-shaped requests through MockMvc, real
Spring Security filter chain including the new rate limiter, real
hand-rolled validators) rather than mocking layers out. **I could not run
these myself** — this sandbox has no Docker/network access, so `mvn test`
please, before relying on them.

**Async delivery dispatch.** `DeliveryService#deliverForEvent` (the path
`MatchingService` calls on every real match) now submits each channel's
send to a dedicated thread pool (`AsyncConfig`) instead of blocking the
ingestion scheduler thread through up to 3 retries with backoff. The manual
`test-notification` endpoint stays synchronous on purpose — it returns
delivery results in the response body, so a caller waiting on that still
needs to actually wait.

**Prod profile.** `application-prod.yml` (activate with
`SPRING_PROFILES_ACTIVE=prod`) removes the dev-only fallback values for
`JWT_SECRET` and `DB_PASSWORD` — the app now fails fast at startup in that
profile if they're not set, rather than silently running with defaults
meant for local dev. It also turns off detailed error bodies and health
details.

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
| `admin_token` | Set automatically by **Auth → Login as Admin**'s test script |
| `channel_id` | Set automatically by **Channels → Create Channel - Slack** |
| `alert_rule_id` | Set automatically by **Alert Rules → Create Alert Rule - Disaster** |
| `news_rule_id` | Set automatically by **Alert Rules → Create Alert Rule - News** (has no channels linked — used to test the "no channels" error) |

**Suggested run order** (each request has test-tab assertions that show
pass/fail in Postman's Test Results):
1. `Auth → Register` then `Auth → Login`. Also run `Auth → Register Admin`
   then `Auth → Login as Admin` — the former sends `"isAdmin": true`, the
   latter saves `{{admin_token}}` for the `Admin` folder below.
2. `Channels → Create Channel - Email` and `Create Channel - Slack` — the
   Slack request saves `{{channel_id}}` (verified will be `false` against
   the placeholder URL, expected).
3. `Alert Rules → Create Alert Rule - News / Market / Disaster` — the
   Disaster request uses `{{channel_id}}` and saves `{{alert_rule_id}}`.
4. `Notifications → Send Test Notification` then `List Deliveries For Rule`.
5. `Admin → Trigger Ingestion Now` and the rest of the `Admin` folder
   (`List Users`, `List All Alert Rules`, `List Events`, `List Deliveries`,
   `Source Status`) — all use `{{admin_token}}` from step 1. Re-run
   `List Deliveries For Rule` (in `Notifications`) afterward if you set up a
   low-threshold DISASTER rule — real matches may have landed there too.
6. `Validation examples (expected to fail)` — deliberately malformed
   requests that should each return the 4xx status asserted in their test
   script.

Run the whole thing unattended with **Runner** (top toolbar) or
`newman run postman/event-alerting-platform.postman_collection.json` if you
have Newman installed.

## Alert rule criteria shapes (hand-rolled per category)

```jsonc
// NEWS
{ "keywords": ["interest rate", "recession"], "match": "any" }  // match: "any" | "all", optional

// MARKET
{ "symbol": "AAPL", "changePercent": { "gte": 5 } }              // gte and/or lte, at least one required — signed, so lte: -5 catches drops

// DISASTER
{ "minMagnitude": 6.0, "region": { "lat": 37.77, "lon": -122.41, "radiusKm": 500 } }  // region optional
```
Anything that doesn't match these shapes is rejected with a 400 before it
reaches the database — see `service/NewsCriteriaValidator.java`,
`MarketCriteriaValidator.java`, `DisasterCriteriaValidator.java`. The
matching-time counterparts (`NewsEventMatcher`, `MarketEventMatcher`,
`DisasterEventMatcher`) apply the same shapes against a real event's payload.

## Sending a notification / triggering one for real

`POST /api/alert-rules/{id}/test-notification` with `{"title": "...", "body": "..."}`
manually sends through every channel linked to the rule. Real matches from
ingestion go through the identical retry/logging code automatically — see
"Event ingestion" above. Either way, `GET /api/alert-rules/{id}/deliveries`
shows the resulting log.

## Running tests
No manual setup needed anymore — Testcontainers spins up Postgres automatically:
```bash
mvn test
```
Requires Docker to be running on whatever machine runs the tests (same as
Testcontainers always does). If Docker isn't available, these tests can't run.
