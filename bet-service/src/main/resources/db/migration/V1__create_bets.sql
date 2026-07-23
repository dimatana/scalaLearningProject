CREATE TABLE IF NOT EXISTS bets (
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL,
    stake       NUMERIC(12, 2) NOT NULL,
    odds        NUMERIC(6, 2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
    );