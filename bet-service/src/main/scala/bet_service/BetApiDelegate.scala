package bet_service

import cats.effect.IO
import fs2.kafka.KafkaProducer
import org.http4s.{Request, Response, Status}
import contracts.BetPlaced
import org.typelevel.log4cats.Logger

import bet_service.generated.server.apis.DefaultApiDelegate
import bet_service.generated.server.apis.DefaultApiDelegate.*
import bet_service.generated.server.models.{
  Bet as GenBet,
  PlaceBetRequest as GenPlaceBetRequest,
  ErrorResponse as GenErrorResponse
}

import java.time.ZoneOffset

final class BetApiDelegate(
  repo: BetRepository,
  producer: KafkaProducer[IO, String, BetPlaced],
  betPlacedTopic: String
)(using logger: Logger[IO])
  extends DefaultApiDelegate[IO]:

  private def toGenBet(bet: Bet): GenBet =
    GenBet(
      id = bet.id,
      eventUnderscoreid = bet.eventId,
      stake = bet.stake.toDouble,
      odds = bet.odds.toDouble,
      createdUnderscoreat = bet.createdAt.atZone(ZoneOffset.UTC)
    )

  private def toGenError(err: BetError): GenErrorResponse =
    GenErrorResponse(error = err.message)

  override def getBet: getBet = new getBet:
    def handle(req: Request[IO], id: java.util.UUID, responses: getBetResponses[IO]): IO[Response[IO]] =
      for
        result <- repo.findById(id)
        response <- result match
          case Right(bet) => responses.resp200(toGenBet(bet))
          case Left(err) =>
            ErrorMapping.toStatus(err) match
              case Status.NotFound => responses.resp404(toGenError(err))
              case _               => responses.resp500(toGenError(err))
      yield response

  override def getHealth: getHealth = new getHealth:
    def handle(req: Request[IO], responses: getHealthResponses[IO]): IO[Response[IO]] =
      responses.resp200()

  override def listBets: listBets = new listBets:
    def handle(req: Request[IO], responses: listBetsResponses[IO]): IO[Response[IO]] =
      for
        result <- repo.findAll()
        response <- result match
          case Right(bets) => responses.resp200(bets.map(toGenBet))
          case Left(err)   => responses.resp500(toGenError(err))
      yield response

  override def placeBet: placeBet = new placeBet:
    def handle(
      req: Request[IO],
      placeBetIO: IO[GenPlaceBetRequest],
      responses: placeBetResponses[IO]
    ): IO[Response[IO]] =
      for
        decoded <- placeBetIO.attempt
        response <- decoded match
          case Left(_) =>
            responses.resp422(GenErrorResponse(error = "invalid request body"))
          case Right(body) =>
            Bet.create(body.eventUnderscoreid, BigDecimal(body.stake), BigDecimal(body.odds)) match
              case Left(err) =>
                ErrorMapping.toStatus(err) match
                  case Status.UnprocessableEntity => responses.resp422(toGenError(err))
                  case _                          => responses.resp500(toGenError(err))
              case Right(bet) =>
                for
                  inserted <- repo.insert(bet)
                  insertedResponse <- inserted match
                    case Left(err) => responses.resp500(toGenError(err))
                    case Right(saved) =>
                      BetEventProducer.publish(producer, saved, betPlacedTopic) *> responses.resp201(toGenBet(saved))
                yield insertedResponse
      yield response
