-- V2: deliveries.event_id becomes nullable.
--
-- Real event ingestion doesn't exist yet (that's M4), but the notification
-- layer (M3) needs to be testable now via a manual "send test notification"
-- endpoint. Those test sends aren't tied to any row in `events`, so the FK
-- can no longer be mandatory. Once M4 lands, ingestion-triggered deliveries
-- will still always set event_id — this only relaxes the constraint for the
-- manual-test path.

ALTER TABLE deliveries ALTER COLUMN event_id DROP NOT NULL;
