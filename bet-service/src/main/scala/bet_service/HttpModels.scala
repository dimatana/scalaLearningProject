package bet_service

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import java.util.UUID

final case class PlaceBetRequest(eventId: UUID, stake: BigDecimal, odds: BigDecimal)

object PlaceBetRequest:
  given Decoder[PlaceBetRequest] = deriveDecoder

final case class ErrorResponse(error: String)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder
