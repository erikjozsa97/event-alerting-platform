# Global Event Alerting Platform — v1 Project Plan (Draft)

**Stack:** Java 17 · Spring Boot · PostgreSQL · Docker · IntelliJ IDEA

## Confirmed decisions

| Question           | Decision                                                                                                         |
| ------------------ | ---------------------------------------------------------------------------------------------------------------- |
| MVP scope          | All three categories from day one: news, market movements, natural disasters                                     |
| "Importance" logic | User-defined rules — each user sets keywords/topics (news), thresholds (markets), magnitude/location (disasters) |
| Admin view         | Secured REST API only — no UI in v1                                                                              |
| Audience           | Public self-serve — open sign-up, users manage their own alert rules                                             |
| Scale (v1)         | Small (dozens of users) — scheduled polling is sufficient, no message queue needed yet                           |

## Architecture

**Modular monolith**, single deployable Spring Boot app + Postgres, run via Docker Compose. Not microservices — at this scale it adds ops overhead for no real benefit, and the two things that actually need to stay flexible (notification channels, event sources) are handled with interfaces, not service boundaries. If usage grows later, ingestion or notification can be pulled into a separate service without touching the domain model, because they're already isolated behind interfaces.

**Two abstractions carry the "must be extensible" requirement:**

```java
public interface EventSource {
    Category getCategory();
    List<RawEvent> fetchLatest();
}

public interface NotificationChannel {
    ChannelType getType();
    DeliveryResult send(User user, Event event, ChannelConfig config);
}
```

Adding a new data source (e.g. weather) or a new channel (e.g. SMS/Discord/webhook) later means writing one new class and registering it — no changes to the matching engine, the API, or the schema.

**Suggested package layout:**

```
auth/          registration, login, JWT, roles
domain/        User, AlertRule, Channel, Event, Delivery entities
alertrule/     rule CRUD + per-category criteria validation
channel/       channel CRUD (email, Slack, future channels)
ingestion/     EventSource impls + scheduler (polling)
matching/      evaluates new Events against active AlertRules
notification/  NotificationChannel impls + dispatch
delivery/      orchestrates match -> notify -> log
admin/         read-only admin REST endpoints (ROLE_ADMIN)
```

**Ingestion at this scale:** Spring `@Scheduled` polling per source (e.g. every 1–2 min for USGS, every few min for news/market APIs) is enough — no Kafka/RabbitMQ needed for dozens of users. New events get deduplicated by `(source, external_id)` and stored once; matching and delivery happen synchronously right after ingestion.

**Auth:** Spring Security + JWT (stateless, fine for a REST-only product). `USER` and `ADMIN` roles; admin endpoints gated by role.

## Draft schema (starting point, not final)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- USER | ADMIN
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,        -- EMAIL | SLACK | ...
    config JSONB NOT NULL,            -- e.g. {"webhookUrl": "..."} or {} for email (uses account email)
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(20) NOT NULL,    -- NEWS | MARKET | DISASTER
    name VARCHAR(255) NOT NULL,
    criteria JSONB NOT NULL,          -- category-specific, validated at the API layer
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alert_rule_channels (   -- a rule can fan out to multiple channels
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id),
    channel_id UUID NOT NULL REFERENCES channels(id),
    PRIMARY KEY (alert_rule_id, channel_id)
);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(50) NOT NULL,      -- e.g. usgs, finnhub, newsapi
    external_id VARCHAR(255) NOT NULL,
    category VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,           -- normalized event data
    occurred_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source, external_id)
);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id),
    event_id UUID NOT NULL REFERENCES events(id),
    channel_id UUID NOT NULL REFERENCES channels(id),
    status VARCHAR(20) NOT NULL,      -- PENDING | SENT | FAILED
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message TEXT
);
```

Example `criteria` payloads (kept in the app layer, not the DB):

- News: `{"keywords": ["interest rate", "recession"], "match": "any"}`
- Market: `{"symbol": "AAPL", "changePercent": {"gte": 5}, "window": "1d"}`
- Disaster: `{"type": "earthquake", "minMagnitude": 6.0, "region": {"lat": ..., "lon": ..., "radiusKm": 500}}`

## API sketch

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/alert-rules
POST   /api/alert-rules
PUT    /api/alert-rules/{id}
DELETE /api/alert-rules/{id}

GET    /api/channels
POST   /api/channels
DELETE /api/channels/{id}

GET    /api/admin/users              (ROLE_ADMIN)
GET    /api/admin/alert-rules        (ROLE_ADMIN)
GET    /api/admin/events?category=&since=   (ROLE_ADMIN)
GET    /api/admin/deliveries?status=&since= (ROLE_ADMIN)
GET    /api/admin/sources/status     (ROLE_ADMIN) -- health/last-poll-time per EventSource
```

## Milestones

| #   | Milestone              | Deliverable                                                                                                   |
| --- | ---------------------- | ------------------------------------------------------------------------------------------------------------- |
| M0  | Project foundation     | Spring Boot skeleton, Docker Compose (app + Postgres), Flyway baseline, health endpoint                       |
| M1  | Auth & users           | Registration/login, JWT, USER/ADMIN roles                                                                     |
| M2  | Alert rules & channels | CRUD APIs, per-category criteria validation, channel setup (email default, Slack webhook + verification ping) |
| M3  | Notification layer     | `NotificationChannel` interface, Email + Slack implementations, delivery logging with retry                   |
| M4  | Event ingestion        | `EventSource` interface + scheduler; connectors for news, market, and disaster data; normalization + dedup    |
| M5  | Rule matching engine   | Evaluate new events against active rules per category; wire matches → notify → log                            |
| M6  | Admin API              | Read-only endpoints for users, rules, events, deliveries, source health                                       |
| M7  | Hardening              | Actuator/Micrometer, integration tests (Testcontainers), rate limiting, OpenAPI docs                          |
