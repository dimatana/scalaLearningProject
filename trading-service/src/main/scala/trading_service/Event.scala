package trading_service

import java.util.UUID

case class Event(
  eventId: UUID,
  name: String,
  betsPlaced: Int
)
