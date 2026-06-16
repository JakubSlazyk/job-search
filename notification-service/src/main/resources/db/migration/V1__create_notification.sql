-- Phase 2.4 notification-service schema. Reactive runtime access is via R2DBC; this migration runs
-- once over a plain JDBC connection at startup (Flyway can't use R2DBC).

-- A user's match rule: the keyword is matched case-insensitively as a substring of an offer's title
-- or company. A user may have many. Self-scoped by the Keycloak `subject`.
CREATE TABLE notification_criteria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject    VARCHAR(255) NOT NULL,
    keyword    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_criteria_subject ON notification_criteria (subject);

-- Idempotency guard for the offer.published consumer: an event_id is applied at most once despite
-- at-least-once Kafka delivery (mirrors tracker-service).
CREATE TABLE processed_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Delivery history: one row per (subject, offer) match that was pushed. Backs GET /notifications and
-- a reconnecting WebSocket client's catch-up.
CREATE TABLE delivered_notifications (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject      VARCHAR(255) NOT NULL,
    offer_id     VARCHAR(255) NOT NULL,
    title        TEXT,
    company      TEXT,
    url          TEXT,
    delivered_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_delivered_notifications_subject ON delivered_notifications (subject, delivered_at DESC);
