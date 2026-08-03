package bet_service

import fs2.kafka.Serializer
import io.circe.syntax.*
import cats.effect.IO
import contracts.BetPlaced

given betPlacedSerializer: Serializer[IO, BetPlaced] =
  Serializer.lift[IO, BetPlaced](event => IO.pure(event.asJson.noSpaces.getBytes("UTF-8")))
