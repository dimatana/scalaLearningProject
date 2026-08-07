package trading_service

import munit.FunSuite
import java.util.UUID

class EventErrorSpec extends FunSuite:

  test("NotFound.message contains the id"):
    val id = UUID.fromString("22222222-2222-2222-2222-222222222222")
    assert(EventError.NotFound(id).message.contains(id.toString))

  test("PersistenceFailure.message contains the cause message"):
    val cause = new RuntimeException("db timeout")
    assert(EventError.PersistenceFailure(cause).message.contains("db timeout"))

  test("PersistenceFailure with null cause message does not throw"):
    val cause = new RuntimeException(null.asInstanceOf[String])
    assert(EventError.PersistenceFailure(cause).message.nonEmpty)
