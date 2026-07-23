package bet_service

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

final class BetRepository(xa: Transactor[IO]):

  def insert(bet: Bet): IO[Either[BetError, Bet]] =
    sql"""
      INSERT INTO bets (id, event_id, stake, odds, created_at)
      VALUES (${bet.id}, ${bet.eventId}, ${bet.stake}, ${bet.odds}, ${bet.createdAt})
    """.update.run
      .transact(xa)
      .attempt
      .map { result =>
        result match
          case Right(_)    => bet.asRight
          case Left(cause) => BetError.RepositoryFailure(cause).asLeft
      }

  def findById(id: UUID): IO[Either[BetError, Bet]] =
    sql"""
      SELECT id, event_id, stake, odds, created_at
      FROM bets
      WHERE id = $id
    """.query[Bet].option
      .transact(xa)
      .attempt
      .map { result =>
        result match
          case Right(Some(bet)) => bet.asRight
          case Right(None)      => BetError.NotFound(id).asLeft
          case Left(cause)      => BetError.RepositoryFailure(cause).asLeft
      }

  def findAll(): IO[Either[BetError, List[Bet]]] =
    sql"""
      SELECT id, event_id, stake, odds, created_at
      FROM bets
      ORDER BY created_at DESC
    """.query[Bet].to[List]
      .transact(xa)
      .attempt
      .map { result =>
        result match
          case Right(bets) => bets.asRight
          case Left(cause) => BetError.RepositoryFailure(cause).asLeft
      }  

object BetRepository:
  given doobie.Read[Bet] =
    doobie.Read[(UUID, UUID, BigDecimal, BigDecimal, java.time.Instant)]
      .map((id, eventId, stake, odds, createdAt) => Bet(id, eventId, stake, odds, createdAt))