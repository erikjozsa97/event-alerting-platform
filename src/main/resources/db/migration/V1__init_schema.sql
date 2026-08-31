-- V1: baseline schema for the event alerting platform
-- Matches the entities in the v1 project plan: users, channels, alert_rules,
-- alert_rule_channels, events, deliveries.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER', -- USER | ADMIN
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE channels (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,             -- EMAIL | SLACK | ...
    config     JSONB NOT NULL DEFAULT '{}'::jsonb, -- e.g. {"webhookUrl": "..."}
    verified   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alert_rules (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category   VARCHAR(20) NOT NULL,             -- NEWS | MARKET | DISASTER
    name       VARCHAR(255) NOT NULL,
    criteria   JSONB NOT NULL,                   -- category-specific, validated in the app layer
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alert_rule_channels (
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    channel_id    UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    PRIMARY KEY (alert_rule_id, channel_id)
);

CREATE TABLE events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source      VARCHAR(50) NOT NULL,            -- e.g. usgs, finnhub, newsapi
    external_id VARCHAR(255) NOT NULL,
    category    VARCHAR(20) NOT NULL,
    payload     JSONB NOT NULL,                  -- normalized event data
    occurred_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source, external_id)
);

CREATE TABLE deliveries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_rule_id  UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    event_id       UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    channel_id     UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL,         -- PENDING | SENT | FAILED
    attempted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message  TEXT
);

CREATE INDEX idx_channels_user_id ON channels(user_id);
CREATE INDEX idx_alert_rules_user_id ON alert_rules(user_id);
CREATE INDEX idx_alert_rules_category_active ON alert_rules(category, active);
CREATE INDEX idx_events_category_occurred_at ON events(category, occurred_at);
CREATE INDEX idx_deliveries_alert_rule_id ON deliveries(alert_rule_id);
