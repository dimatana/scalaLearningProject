package trading_service

import io.circe.parser.decode
import io.circe.Error as CirceError
import contracts.BetPlaced

sealed trait DecodeError
object DecodeError:
  final case class InvalidJson(cause: CirceError) extends DecodeError
  case object EmptyPayload extends DecodeError
  
object RecordDecoder:
  def decodeRecord(bytes: Array[Byte]): Either[DecodeError, BetPlaced] =
    if bytes == null || bytes.isEmpty then
      Left(DecodeError.EmptyPayload)
    else
      decode[BetPlaced](new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
        .left.map(DecodeError.InvalidJson.apply)  
