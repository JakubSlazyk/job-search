-- Phase 2.3 tracker-service baseline.

-- The user's tracked offers (applications), self-scoped by the Keycloak subject. (subject, offer_id)
-- is the natural key: a user tracks any given offer at most once.
CREATE TABLE tracked_offers (
    subject    VARCHAR(255) NOT NULL,
    offer_id   VARCHAR(255) NOT NULL,
    status     TEXT         NOT NULL,
    notes      TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (subject, offer_id)
);

-- Local denormalized copy of offer display fields, fed by `offer.published` (enrichment). No FK to
-- tracked_offers: a snapshot may arrive before/after any user tracks the offer, in either order.
CREATE TABLE offer_snapshots (
    offer_id     VARCHAR(255) PRIMARY KEY,
    title        TEXT,
    company      TEXT,
    url          TEXT,
    published_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotency guard for at-least-once delivery: an event_id seen here has already been applied.
CREATE TABLE processed_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
