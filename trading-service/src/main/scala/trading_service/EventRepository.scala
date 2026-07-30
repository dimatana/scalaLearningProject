package trading_service

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

final class EventRepository(xa: Transactor[IO]):

  def findById(id: Long): IO[Either[EventError, Event]] =
    sql"""
      SELECT event_id, name, bets_placed
      FROM events
      WHERE event_id = $id
    """.query[Event].option
      .transact(xa)
      .attempt
      .map {
        case Right(Some(event)) => event.asRight
        case Right(None)        => EventError.NotFound(id).asLeft
        case Left(cause)        => EventError.RepositoryFailure(cause).asLeft
      }

object EventRepository:
  given doobie.Read[Event] =
    doobie.Read[(Long, String, Int)]
      .map((id, name, betsPlaced) => Event(id, name, betsPlaced))