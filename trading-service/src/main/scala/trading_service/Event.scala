package trading_service

case class Event(
  eventId: Long,
  name: String,
  betsPlaced: Int
)