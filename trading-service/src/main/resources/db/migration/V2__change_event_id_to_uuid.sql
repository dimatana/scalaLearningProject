DROP TABLE IF EXISTS events;

CREATE TABLE events (
  event_id    UUID PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  bets_placed INT NOT NULL DEFAULT 0
);

INSERT INTO events (event_id, name) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Champions League Final'),
  ('22222222-2222-2222-2222-222222222222', 'World Cup Quarter Final');