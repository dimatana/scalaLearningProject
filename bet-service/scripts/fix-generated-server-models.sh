#!/usr/bin/env sh
set -eu

FILE="bet-service/generated/server/src/main/scala/bet_service/generated/server/models/types.scala"

if [ ! -f "$FILE" ]; then
  echo "Missing generated models file: $FILE" >&2
  exit 1
fi

if ! grep -q '^object Bet {$' "$FILE"; then
  echo "Expected generated block 'object Bet' was not found in $FILE" >&2
  exit 1
fi

if ! grep -q '^object PlaceBetRequest {$' "$FILE"; then
  echo "Expected generated block 'object PlaceBetRequest' was not found in $FILE" >&2
  exit 1
fi

sed -i '/^object Bet {$/,/^}$/c\
object Bet {\
  implicit val encoderBet: Encoder[Bet] =\
    Encoder.forProduct5("id", "event_id", "stake", "odds", "created_at")(b =>\
      (b.id, b.eventUnderscoreid, b.stake, b.odds, b.createdUnderscoreat)\
    )\
\
  implicit val decoderBet: Decoder[Bet] = Decoder.instance { c =>\
    for\
      id <- c.downField("id").as[UUID]\
      eventId <- c.downField("event_id").as[UUID].orElse(c.downField("eventUnderscoreid").as[UUID])\
      stake <- c.downField("stake").as[Double]\
      odds <- c.downField("odds").as[Double]\
      createdAt <- c.downField("created_at").as[ZonedDateTime].orElse(c.downField("createdUnderscoreat").as[ZonedDateTime])\
    yield Bet(\
      id = id,\
      eventUnderscoreid = eventId,\
      stake = stake,\
      odds = odds,\
      createdUnderscoreat = createdAt\
    )\
  }\
}' "$FILE"

sed -i '/^object PlaceBetRequest {$/,/^}$/c\
object PlaceBetRequest {\
  implicit val encoderPlaceBetRequest: Encoder[PlaceBetRequest] =\
    Encoder.forProduct3("event_id", "stake", "odds")(r => (r.eventUnderscoreid, r.stake, r.odds))\
\
  implicit val decoderPlaceBetRequest: Decoder[PlaceBetRequest] = Decoder.instance { c =>\
    for\
      eventId <- c.downField("event_id").as[UUID].orElse(c.downField("eventUnderscoreid").as[UUID])\
      stake <- c.downField("stake").as[Double]\
      odds <- c.downField("odds").as[Double]\
    yield PlaceBetRequest(\
      eventUnderscoreid = eventId,\
      stake = stake,\
      odds = odds\
    )\
  }\
}' "$FILE"
