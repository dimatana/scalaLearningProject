CREATE TABLE processed_bets (
    bet_id       UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);