package bet_service

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

final case class Bet(
  id: UUID,
  bettor: String,
  stake: BigDecimal,
  odds: BigDecimal,
  status: String
)

object Bet:
  given Encoder[Bet] = deriveEncoder
  given Decoder[Bet] = deriveDecoder
  
final case class CreateBetRequest(  
  bettor: String,
  stake: BigDecimal,
  odds: BigDecimal
)

object CreateBetRequest:
  given Encoder[CreateBetRequest] = deriveEncoder
  given Decoder[CreateBetRequest] = deriveDecoder