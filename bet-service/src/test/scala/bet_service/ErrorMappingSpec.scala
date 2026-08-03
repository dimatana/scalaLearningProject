package bet_service

import munit.FunSuite
import org.http4s.Status
import java.util.UUID

class ErrorMappingSpec extends FunSuite:

  test("NotFound maps to 404"):
    assertEquals(ErrorMapping.toStatus(BetError.NotFound(UUID.randomUUID())), Status.NotFound)

  test("InvalidStake maps to 422"):
    assertEquals(ErrorMapping.toStatus(BetError.InvalidStake("stake must be positive")), Status.UnprocessableEntity)

  test("InvalidOdds maps to 422"):
    assertEquals(
      ErrorMapping.toStatus(BetError.InvalidOdds("odds must be greater than 1.0")),
      Status.UnprocessableEntity
    )

  test("RepositoryFailure maps to 500"):
    assertEquals(
      ErrorMapping.toStatus(BetError.RepositoryFailure(new RuntimeException("db timeout"))),
      Status.InternalServerError
    )
