-- Extra per-service databases on the shared Postgres container (one DB per service; ADR 0001).
-- Runs only on a FRESH data volume (Docker's /docker-entrypoint-initdb.d contract) — if you already
-- have a local volume, recreate it once with `docker compose down -v`. `offers` is created by the
-- container's POSTGRES_DB. tracker/notification are pre-created for Phase 2.3/2.4. Owned by the
-- existing `offers` role, which every service connects as locally.
CREATE DATABASE users OWNER offers;
CREATE DATABASE tracker OWNER offers;
CREATE DATABASE notification OWNER offers;
