CREATE TABLE bets (
                      id          UUID PRIMARY KEY,
                      bettor_id   UUID NOT NULL,
                      amount      NUMERIC(12, 2) NOT NULL,
                      status      TEXT NOT NULL,
                      created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);