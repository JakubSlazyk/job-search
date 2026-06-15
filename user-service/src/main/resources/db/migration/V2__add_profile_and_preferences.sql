-- Phase 2.2: enrich the thin 2.1 users table with editable profile fields, and add a 1:1
-- preferences table. Profile columns are user-edited, so the login upsert never overwrites them.
ALTER TABLE users
    ADD COLUMN display_name TEXT,
    ADD COLUMN full_name    TEXT,
    ADD COLUMN headline     TEXT,
    ADD COLUMN phone        TEXT,
    ADD COLUMN location     TEXT,
    ADD COLUMN linkedin_url TEXT,
    ADD COLUMN github_url   TEXT,
    ADD COLUMN website_url  TEXT;

-- Contact/channel preferences (notification *matching criteria* live in notification-service, §2.4).
-- A default row is provisioned alongside the user on first login.
CREATE TABLE user_preferences (
    subject                     VARCHAR(255) PRIMARY KEY REFERENCES users (subject) ON DELETE CASCADE,
    email_notifications_enabled BOOLEAN     NOT NULL DEFAULT true,
    locale                      TEXT        NOT NULL DEFAULT 'en',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
