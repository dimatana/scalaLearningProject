package trading_service

import java.util.UUID

enum EventError:
  case NotFound(id: UUID)
  case PersistenceFailure(cause: Throwable)

  def message: String = this match
    case NotFound(id) => s"Event $id not found"
    case PersistenceFailure(cause) =>
      Option(cause.getMessage).getOrElse("Unknown persistence error")
