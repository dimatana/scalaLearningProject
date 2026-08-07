package trading_service

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

enum ProcessResult:
  case Counted
  case AlreadyProcessed

final class EventRepository(xa: Transactor[IO]):

  /** Finds an event by id or returns a typed not-found error. */
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
        case Left(cause)        => EventError.PersistenceFailure(cause).asLeft
      }

  /** Increments the bets counter for an event. */
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
        case Right(_)                                => EventError.NotFound(eventId).asLeft
        case Left(cause)                             => EventError.PersistenceFailure(cause).asLeft
      }

  /** Increments event counter only if the bet id was not processed before. */
  def incrementIfNotProcessed(betId: UUID, eventId: UUID): IO[Either[EventError, ProcessResult]] =
    val insertGuard: ConnectionIO[Int] =
      sql"INSERT INTO processed_bets (bet_id) VALUES ($betId) ON CONFLICT DO NOTHING"
        .update.run

    val incrementCounter: ConnectionIO[Either[EventError, ProcessResult]] =
      sql"UPDATE events SET bets_placed = bets_placed + 1 WHERE event_id = $eventId"
        .update.run
        .map { rowsUpdated =>
          if rowsUpdated > 0 then ProcessResult.Counted.asRight
          else EventError.NotFound(eventId).asLeft
        }

    val program: ConnectionIO[Either[EventError, ProcessResult]] =
      insertGuard.flatMap {
        case 0 => ProcessResult.AlreadyProcessed.asRight[EventError].pure[ConnectionIO]
        case _ => incrementCounter
      }

    program.transact(xa).attempt.map {
      case Right(result) => result
      case Left(cause)   => EventError.PersistenceFailure(cause).asLeft
    }

object EventRepository:

  given doobie.Read[Event] =
    doobie.Read[(UUID, String, Int)]
      .map((id, name, betsPlaced) => Event(id, name, betsPlaced))
