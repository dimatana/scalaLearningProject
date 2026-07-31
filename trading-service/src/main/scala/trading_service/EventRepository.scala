package trading_service

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

final class EventRepository(xa: Transactor[IO]):

  def findById(id: UUID): IO[Either[EventError, Event]] =
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

  def incrementBetsPlaced(eventId: UUID): IO[Either[EventError, Unit]] =
    sql"""
        UPDATE events
        SET bets_placed = bets_placed + 1
        WHERE event_id = $eventId
      """.update.run
      .transact(xa)
      .attempt
      .map {
        case Right(rowsAffected) if rowsAffected > 0 => ().asRight
        case Right(_) => EventError.NotFound(eventId).asLeft
        case Left(cause) => EventError.RepositoryFailure(cause).asLeft
      }
      
object EventRepository:
  given doobie.Read[Event] =
    doobie.Read[(UUID, String, Int)]
      .map((id, name, betsPlaced) => Event(id, name, betsPlaced))