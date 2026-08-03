package bet_service

import java.time.Instant
import java.util.UUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class Bet(
  id: UUID,
  eventId: UUID,
  stake: BigDecimal,
  odds: BigDecimal,
  createdAt: Instant
)

object Bet:
  given Encoder[Bet] = deriveEncoder

  def create(eventId: UUID, stake: BigDecimal, odds: BigDecimal): Either[BetError, Bet] =
    for
      validStake <- Either.cond(stake > 0, stake, BetError.InvalidStake("stake must be positive"))
      validOdds  <- Either.cond(odds > 1.0, odds, BetError.InvalidOdds("odds must be greater than 1.0"))
    yield Bet(
      id = UUID.randomUUID(),
      eventId = eventId,
      stake = validStake,
      odds = validOdds,
      createdAt = Instant.now()
    )
