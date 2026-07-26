package bet_service

import org.http4s.Status


object ErrorMapping:
  def toStatus(e: BetError): Status = e match
  case BetError.NotFound(_)         => Status.NotFound
  case BetError.InvalidStake(_)     => Status.UnprocessableEntity
  case BetError.InvalidOdds(_)      => Status.UnprocessableEntity
  case BetError.RepositoryFailure(_) => Status.InternalServerError