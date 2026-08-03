package contracts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

import java.time.Instant
import java.util.UUID

final case class BetPlaced(
  betId: UUID,
  eventId: UUID,
  stake: BigDecimal,
  odds: BigDecimal,
  occurredAt: Instant
)

object BetPlaced:
  given Encoder[BetPlaced] = deriveEncoder
  given Decoder[BetPlaced] = deriveDecoder
