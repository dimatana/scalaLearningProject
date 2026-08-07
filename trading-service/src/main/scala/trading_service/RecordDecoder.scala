package trading_service

import io.circe.parser.decode
import io.circe.Error as CirceError
import contracts.BetPlaced

sealed trait DecodeError

object DecodeError:
  case object MissingPayload                      extends DecodeError
  case object EmptyPayload                        extends DecodeError
  final case class InvalidJson(cause: CirceError) extends DecodeError

object RecordDecoder:

  /** Decodes a Kafka payload into a BetPlaced contract model. */
  def decodeRecord(payload: Option[Array[Byte]]): Either[DecodeError, BetPlaced] =
    payload match {
      case None                         => Left(DecodeError.MissingPayload)
      case Some(bytes) if bytes.isEmpty => Left(DecodeError.EmptyPayload)
      case Some(bytes) =>
        decode[BetPlaced](new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
          .left.map(DecodeError.InvalidJson.apply)
    }
