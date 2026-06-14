-- Transactional outbox (ADR 0002, phase §1.5). offer-service writes an OfferPublished event row here
-- in the SAME transaction as the offers upsert; Debezium tails the WAL and drains it to
-- `offer.published`. Column names match the Debezium Outbox Event Router SMT defaults so the
-- connector needs no field-name overrides (key is taken from aggregateid).
CREATE TABLE outbox (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregatetype VARCHAR(255) NOT NULL,   -- routing discriminator (e.g. 'offer')
    aggregateid   VARCHAR(255) NOT NULL,   -- offer_id -> Kafka message key
    type          VARCHAR(255) NOT NULL,   -- event type (e.g. 'OfferPublished')
    payload       BYTEA        NOT NULL,   -- serialized OfferPublished protobuf (raw wire bytes)
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Supports the scheduled purge of already-drained rows by age.
CREATE INDEX idx_outbox_created_at ON outbox (created_at);
