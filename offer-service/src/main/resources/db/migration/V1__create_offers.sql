-- Canonical offer store (write side). The natural key is offer_id = "<source>:<external_id>".
CREATE TABLE offers (
    offer_id    VARCHAR(255) PRIMARY KEY,
    source      VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    title       TEXT        NOT NULL,
    company     TEXT        NOT NULL,
    url         TEXT        NOT NULL,
    location    TEXT        NOT NULL,
    description TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
