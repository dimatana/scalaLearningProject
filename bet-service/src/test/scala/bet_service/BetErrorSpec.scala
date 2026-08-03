package bet_service

import munit.FunSuite
import java.util.UUID

class BetErrorSpec extends FunSuite:

  test("NotFound.message contains the id"):
    val id = UUID.fromString("11111111-1111-1111-1111-111111111111")
    assert(BetError.NotFound(id).message.contains(id.toString))

  test("InvalidStake.message contains the reason"):
    val reason = "stake must be positive"
    assert(BetError.InvalidStake(reason).message.contains(reason))

  test("InvalidOdds.message contains the reason"):
    val reason = "odds must be greater than 1.0"
    assert(BetError.InvalidOdds(reason).message.contains(reason))

  test("RepositoryFailure.message contains the cause message"):
    val cause = new RuntimeException("connection refused")
    assert(BetError.RepositoryFailure(cause).message.contains("connection refused"))

  test("RepositoryFailure with null cause message does not throw"):
    val cause = new RuntimeException(null.asInstanceOf[String])
    assert(BetError.RepositoryFailure(cause).message.nonEmpty)
