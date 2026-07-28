package contracts

import munit.FunSuite
import io.circe.syntax.*
import io.circe.parser.decode
import java.time.Instant
import java.util.UUID

class BetPlacedCodecSpec extends FunSuite:

  test("BetPlaced round-trips through encode -> decode"):
    val original = BetPlaced(
      betId = UUID.randomUUID(),
      eventId = UUID.randomUUID(),
      stake = BigDecimal("100.00"),
      odds = BigDecimal("1.85"),
      occurredAt = Instant.parse("2026-07-27T10:00:00Z")
    )

    val json    = original.asJson.noSpaces
    val decoded = decode[BetPlaced](json)

    assertEquals(decoded, Right(original))

  test("BetPlaced decode fails gracefully on malformed JSON"):
    val malformed = """{"betId": "not-a-uuid"}"""
    assert(decode[BetPlaced](malformed).isLeft)