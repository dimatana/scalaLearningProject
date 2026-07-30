CREATE TABLE IF NOT EXISTS events (
    event_id    BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    bets_placed INT NOT NULL DEFAULT 0
    );

INSERT INTO events (name) VALUES ('Champions League Final');
INSERT INTO events (name) VALUES ('World Cup Quarter Final');