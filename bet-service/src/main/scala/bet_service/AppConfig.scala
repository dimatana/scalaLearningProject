package bet_service

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.FiniteDuration

final case class AppConfig(
    httpPort: Int,
    httpShutdownTimeout: FiniteDuration,
    dbUrl: String,
    dbUser: String,
    dbPassword: String,
    dbMaxPoolSize: Int,
    dbMinIdle: Int,
    dbConnectionTimeout: FiniteDuration,
    kafkaBrokers: String,
    betPlacedTopic: String
  ) derives ConfigReader

object AppConfig:
  def load(): AppConfig =
    pureconfig.ConfigSource.default.loadOrThrow[AppConfig]