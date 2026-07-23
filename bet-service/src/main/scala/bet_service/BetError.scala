package bet_service

import java.util.UUID

enum BetError:
  case NotFound(id: UUID)
  case InvalidStake(reason: String)
  case InvalidOdds(reason: String)
  case RepositoryFailure(cause: Throwable)

  def message: String = this match
    case NotFound(id)           => s"Bet with ID $id not found"
    case InvalidStake(reason)   => s"Invalid stake: $reason"
    case InvalidOdds(reason)    => s"Invalid odds: $reason"
    case RepositoryFailure(cause) => s"Repository failure: ${cause.getMessage}"
