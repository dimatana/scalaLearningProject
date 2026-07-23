// BetValidationSpec.scala
import munit.CatsEffectSuite

class BetValidationSpec extends CatsEffectSuite:

  private val validSelection = Selection("event1", "1x2", 1.85)

  test("happy path: stake valid + selecție validă => payout corect") {
    assertIO(
      BetValidation.calculatePayoutIO(50.0, List(validSelection)),
      Right(92.5)  // 50 * 1.85
    )
  }

  test("stake zero => InvalidStake") {
    assertIO(
      BetValidation.calculatePayoutIO(0.0, List(validSelection)),
      Left(BetError.InvalidStake(0.0))
    )
  }

  test("stake negativ => InvalidStake") {
    assertIO(
      BetValidation.calculatePayoutIO(-10.0, List(validSelection)),
      Left(BetError.InvalidStake(-10.0))
    )
  }

  test("cotă sub prag (<=1.01) => InvalidOdds") {
    val badSelection = Selection("event2", "draw", 1.0)
    assertIO(
      BetValidation.calculatePayoutIO(50.0, List(badSelection)),
      Left(BetError.InvalidOdds(1.0))
    )
  }

  test("listă de selecții goală => EmptySelections") {
    assertIO(
      BetValidation.calculatePayoutIO(50.0, List.empty),
      Left(BetError.EmptySelections)
    )
  }

  test("combo bet: payout se calculează ca produs al cotelor selecțiilor") {
    val selections = List(
      Selection("event1", "1x2", 2.0),
      Selection("event2", "1x2", 1.5)
    )
    assertIO(
      BetValidation.calculatePayoutIO(10.0, selections),
      Right(30.0)  // 10 * 2.0 * 1.5
    )
  }