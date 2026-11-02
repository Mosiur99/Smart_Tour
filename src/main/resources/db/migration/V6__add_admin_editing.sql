-- Each save raises the revision, so a save made from an out-of-date screen is refused
-- instead of silently undoing someone else's change.
ALTER TABLE destination   ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE poi           ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE hotel         ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transport_leg ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;

-- The history page lists the newest changes first.
CREATE INDEX audit_log_created_idx ON audit_log (created_at DESC);
