package trading_service

import java.util.UUID

sealed trait EventError:
  def message: String

object EventError:
  case class NotFound(id: UUID) extends EventError:
    def message: String = s"Event $id not found"

  case class RepositoryFailure(cause: Throwable) extends EventError:
    def message: String = Option(cause.getMessage).getOrElse("Unknown repository error")