-- Seniority is derived by processing-service (§1.3) and carried on normalized-offers.
ALTER TABLE offers ADD COLUMN seniority VARCHAR(32) NOT NULL DEFAULT 'SENIORITY_UNSPECIFIED';
