package bet_service

import java.util.UUID

class BetSpec extends munit.FunSuite:

  private val eventId = UUID.randomUUID()

  test("happy case - valid stake and odds produce a Bet"):
    val result = Bet.create(eventId, BigDecimal(50), BigDecimal(2.5))
    result match
      case Right(bet) =>
        assertEquals(bet.eventId, eventId)
        assertEquals(bet.stake, BigDecimal(50))
        assertEquals(bet.odds, BigDecimal(2.5))
      case Left(err) =>
        fail(s"expected Right, got Left($err)")

  test("zero stake is rejected"):
    val result = Bet.create(eventId, BigDecimal(0), BigDecimal(2.5))
    assertEquals(result, Left(BetError.InvalidStake("stake must be positive")))

  test("negative stake is rejected"):
    val result = Bet.create(eventId, BigDecimal(-10), BigDecimal(2.5))
    assertEquals(result, Left(BetError.InvalidStake("stake must be positive")))

  test("odds equal to 1.0 are rejected"):
    val result = Bet.create(eventId, BigDecimal(50), BigDecimal(1.0))
    assertEquals(result, Left(BetError.InvalidOdds("odds must be greater than 1.0")))

  test("odds below 1.0 are rejected"):
    val result = Bet.create(eventId, BigDecimal(50), BigDecimal(0.8))
    assertEquals(result, Left(BetError.InvalidOdds("odds must be greater than 1.0")))
