package bet_service

import cats.effect.{IO, Ref}

import java.util.UUID

final class BetStore (state: Ref[IO, Map[UUID, Bet]]):

  def insert(req: CreateBetRequest): IO[Bet] =
    for
      id <- IO.randomUUID
      bet = Bet(id, req.bettor, req.stake, req.odds, status = "pending")
      _ <- state.update(_.updated(id, bet))
    yield bet

  def find(id: UUID): IO[Option[Bet]] =
    state.get.map(_.get(id))

object BetStore:

  def empty: IO[BetStore] =
    Ref.of[IO, Map[UUID, Bet]](Map.empty).map(new BetStore(_))  