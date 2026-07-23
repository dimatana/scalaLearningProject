package bet_service

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

import java.util.UUID

object BetRoutes:

  def routes(store: BetStore): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "bets" =>
      req.as[CreateBetRequest].attempt.flatMap {
        case Right(createReq) =>
          store.insert(createReq).flatMap(bet => Created(bet))
        case Left(_) =>
          BadRequest("Invalid request body")
      }

    case GET -> Root / "bets" / betId =>
      IO(UUID.fromString(betId)).attempt.flatMap {
        case Left(_) =>
          BadRequest(s"Invalid bet id : $betId")
        case Right(id) =>
          store.find(id).flatMap {
            case Some(bet) => Ok(bet)
            case None => NotFound(s"Bet not found: $betId")
          }
      }
  }
