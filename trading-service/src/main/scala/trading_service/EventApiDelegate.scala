package trading_service

import cats.effect.IO
import trading_service.EventError
import org.http4s.{Request, Response}
import org.typelevel.log4cats.Logger
import trading_service.generated.server.apis.DefaultApiDelegate
import trading_service.generated.server.apis.DefaultApiDelegate.*
import trading_service.generated.server.models.{Event as GenEvent, ErrorResponse as GenErrorResponse}

import java.util.UUID

final class EventApiDelegate(repo: EventRepository)(using logger: Logger[IO])
  extends DefaultApiDelegate[IO]:

  private def toGenEvent(e: Event): GenEvent =
    GenEvent(eventId = e.eventId, name = e.name, betsPlaced = e.betsPlaced)

  override def getEvent: getEvent = new getEvent:
    def handle(req: Request[IO], id: UUID, responses: getEventResponses[IO]): IO[Response[IO]] =
      logger.info(s"GET /events/$id") *>
        repo.findById(id).flatMap:
          case Right(event)                 => responses.resp200(toGenEvent(event))
          case Left(_: EventError.NotFound) => responses.resp404(GenErrorResponse(s"Event $id not found"))
          case Left(err)                    => responses.resp500(GenErrorResponse(err.message))

  override def getHealth: getHealth = new getHealth:
    def handle(req: Request[IO], responses: getHealthResponses[IO]): IO[Response[IO]] =
      responses.resp200()
