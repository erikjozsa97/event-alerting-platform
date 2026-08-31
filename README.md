# Event Alerting Platform

Public self-serve platform where users define alert rules for news, market
moves, and natural disasters, and get notified over email/Slack (more
channels later).

**Status:** M0 (foundation), M1 (auth), M2 (alert rules & channels), M3
(notifications), M4 (event ingestion & matching), M6 (admin API) complete.
M5 (rule matching engine) was folded into M4 rather than done separately —
see the note in that section below.

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
    CustomUserDetailsService, CurrentUserService

  exception/    domain exceptions, mapped to HTTP responses by GlobalExceptionHandler
    EmailAlreadyExistsException, InvalidCredentialsException,
    InvalidCriteriaException, InvalidChannelConfigException,
    AlertRuleNotFoundException, ChannelNotFoundException,
    NotificationDeliveryException, NoChannelsLinkedException

src/main/resources/
  application.yml                            config (datasource, JWT secret, mail, ingestion)
  db/migration/V1__init_schema.sql            full v1 schema
  db/migration/V2__deliveries_event_id_nullable.sql

postman/
  event-alerting-platform.postman_collection.json   importable collection, see below
```

### Why a view layer instead of `@JsonIgnore`

Controllers never return `model` objects directly — they return `view`
records, and those records simply don't have a field for anything sensitive.
For `User`, that means no `passwordHash` field on `UserView` at all. For
`Channel`, it goes further: the Slack `webhookUrl` inside the generic
`config` map is a bearer credential, so `ChannelView` masks it before it's
serialized — something `@JsonIgnore` on a top-level field could never do for
something buried inside a `Map`.

### Why `deliveries.event_id` is nullable (V2 migration)

M3 shipped a manual **"send test notification"** endpoint before real
ingestion existed, so it could be tested end to end. Those manual sends have
no `event_id`. Real ingestion (M4) always sets `event_id` — this migration
only opened the door for the manual-test path to coexist with it.

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

## Event ingestion (M4)

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
the rest of `/api/admin/**` — since there's no admin-promotion endpoint yet,
promote your test user directly in Postgres to try it:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'jane@example.com';
```
(`docker exec -it eventalert-postgres psql -U eventalert -d eventalert` gets
you a `psql` shell, or use any Postgres client against `localhost:5432`.)
You'll also need to log in again afterward — the JWT carries the role from
whenever it was issued.

**To actually see a match fire:** create a DISASTER rule with a low
threshold — magnitude 1+ earthquakes happen constantly, so
`{"minMagnitude": 1.0}` (no `region`) against a channel you've linked will
very likely produce a real `SENT`/`FAILED` delivery on the next poll. Check
`GET /api/alert-rules/{id}/deliveries` afterward.

## Admin API (M6)

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

No promotion endpoint exists yet — see the SQL snippet above to make a test
user an ADMIN. `category`/`status` query params match the enum names
exactly (`NEWS`, `MARKET`, `DISASTER` / `SENT`, `FAILED`, `PENDING`); `since`
takes an ISO-8601 timestamp, e.g. `2026-08-01T00:00:00Z`.

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
1. `Auth → Register` then `Auth → Login`.
2. `Channels → Create Channel - Email` and `Create Channel - Slack` — the
   Slack request saves `{{channel_id}}` (verified will be `false` against
   the placeholder URL, expected).
3. `Alert Rules → Create Alert Rule - News / Market / Disaster` — the
   Disaster request uses `{{channel_id}}` and saves `{{alert_rule_id}}`.
4. `Notifications → Send Test Notification` then `List Deliveries For Rule`.
5. `Admin → Trigger Ingestion Now` — returns 403 unless you've promoted your
   test user to ADMIN in Postgres first (see above); 200 with a per-source
   count otherwise. The rest of the `Admin` folder (`List Users`,
   `List All Alert Rules`, `List Events`, `List Deliveries`, `Source Status`)
   needs the same promotion. Re-run `List Deliveries For Rule` (in
   `Notifications`) afterward if you set up a low-threshold DISASTER rule —
   real matches may have landed there too.
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

## Sending a notification (M3) / triggering one for real (M4)

`POST /api/alert-rules/{id}/test-notification` with `{"title": "...", "body": "..."}`
manually sends through every channel linked to the rule. Real matches from
ingestion go through the identical retry/logging code automatically — see
"Event ingestion" above. Either way, `GET /api/alert-rules/{id}/deliveries`
shows the resulting log.

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
- **M4** — `EventSource` abstraction (USGS live, NewsAPI/Finnhub key-gated), scheduled + on-demand polling, dedup on `(source, external_id)`, `EventMatcher` per category, matches wired into M3's delivery path, `deliveries` listing endpoint. **Folded in what was M5** (rule matching engine) rather than doing it as a separate milestone.
- **M6** — Read-only Admin API: users, alert rules (with owner), events, deliveries (with owner), and per-source ingestion health

## Next: M7 — Hardening
Observability (Actuator/Micrometer metrics beyond health), integration tests
with Testcontainers, rate limiting, OpenAPI docs, and a look at whether
`DeliveryService`'s synchronous retry loop needs to move off the
request/scheduler thread now that real ingestion can trigger it at volume.
