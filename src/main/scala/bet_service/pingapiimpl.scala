package bet_service

import cats.effect.IO
import java.time.Instant
import ping.server.models.PingResponse

class PingApiImpl {
  def ping(): IO[PingResponse] =
    IO.pure(PingResponse(status = "pong", timestamp = Instant.now()))
}
