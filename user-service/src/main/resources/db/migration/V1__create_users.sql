-- User records keyed on the Keycloak subject (the JWT `sub` claim) — the stable identity the rest of
-- the platform references. Provisioned on first authenticated call (see UserService.upsertFromToken).
CREATE TABLE users (
    subject      VARCHAR(255) PRIMARY KEY,
    username     TEXT        NOT NULL,
    email        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
