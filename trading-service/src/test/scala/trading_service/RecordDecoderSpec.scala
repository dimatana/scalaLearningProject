package trading_service

import munit.FunSuite

import java.util.UUID

class RecordDecoderSpec extends FunSuite:

  test("valid JSON decodes to BetPlaced") {
    val eventId = UUID.randomUUID()
    val json =
      s"""{"betId":"${UUID.randomUUID()}","eventId":"$eventId","stake":10.5,"odds":1.85,"occurredAt":"2026-07-30T10:00:00Z"}"""
        .getBytes("UTF-8")

    RecordDecoder.decodeRecord(json) match
      case Right(bp) => assertEquals(bp.eventId, eventId)
      case Left(err) => fail(s"expected Right, got Left($err)")
  }

  test("malformed JSON returns Left(InvalidJson)") {
    val bytes = """{"eventId": "not-closed""".getBytes("UTF-8")

    RecordDecoder.decodeRecord(bytes) match
      case Left(_: DecodeError.InvalidJson) => ()
      case other                            => fail(s"expected Left(InvalidJson), got $other")
  }

  test("empty payload returns Left(EmptyPayload)") {
    assertEquals(RecordDecoder.decodeRecord(Array.emptyByteArray), Left(DecodeError.EmptyPayload))
  }

  test("null payload returns Left(EmptyPayload)") {
    assertEquals(RecordDecoder.decodeRecord(null), Left(DecodeError.EmptyPayload))
  }
