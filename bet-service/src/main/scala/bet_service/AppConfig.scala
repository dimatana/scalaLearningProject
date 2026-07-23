package bet_service

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.* 
  
final case class AppConfig(
                            httpPort: Int,
                            dbUrl: String,
                            dbUser: String,
                            dbPassword: String,
                            kafkaBrokers: String
                          ) derives ConfigReader // derives = generează instanța type class-ului automat, ca derive(Deserialize)

object AppConfig:
  def load(): AppConfig =
    pureconfig.ConfigSource.default.loadOrThrow[AppConfig]