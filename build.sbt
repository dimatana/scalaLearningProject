import sbt.Keys.testFrameworks
import sbtassembly.MergeStrategy

ThisBuild / scalaVersion := "3.8.4"
val http4sVersion = "0.23.30"
val circeVersion  = "0.14.10"
val fs2KafkaVersion = "3.5.1"

val commonDeps = Seq(
  "org.typelevel"  %% "cats-effect"         % "3.5.7",
  "org.http4s"     %% "http4s-ember-server" % http4sVersion,
  "org.http4s"     %% "http4s-dsl"          % http4sVersion,
  "org.http4s"     %% "http4s-circe"        % http4sVersion,
  "io.circe"       %% "circe-generic"       % circeVersion,
  "org.typelevel"  %% "log4cats-slf4j"      % "2.7.0",
  "org.typelevel"  %% "log4cats-core"       % "2.7.0",
  "ch.qos.logback" %  "logback-classic"     % "1.5.16",
  "org.tpolecat"   %% "doobie-core"         % "1.0.0-RC5",
  "org.tpolecat"   %% "doobie-hikari"       % "1.0.0-RC5",
  "org.tpolecat"   %% "doobie-postgres"     % "1.0.0-RC5",
  "org.postgresql" %  "postgresql"          % "42.7.3",
  "org.flywaydb"   %  "flyway-core"         % "10.15.0",
  "org.flywaydb"   %  "flyway-database-postgresql" % "10.15.0",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.7",
  "com.github.fd4s" %% "fs2-kafka"          % fs2KafkaVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalameta"  %% "munit"               % "1.0.0"  % Test,
  "org.typelevel"  %% "munit-cats-effect"   % "2.2.0"  % Test
)

lazy val contracts = (project in file("contracts"))
  .settings(
    name := "contracts",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "org.scalameta" %% "munit" % "1.0.0" % Test
    )
  )

lazy val betServiceGeneratedServer = (project in file("bet-service/generated/server"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-effect"  % "3.5.7",
      "org.http4s"     %% "http4s-dsl"    % http4sVersion,
      "org.http4s"     %% "http4s-circe"  % http4sVersion,
      "org.http4s"     %% "http4s-core"   % http4sVersion,
      "org.http4s"     %% "http4s-server" % http4sVersion,
      "io.circe"       %% "circe-core"    % circeVersion,
      "io.circe"       %% "circe-generic" % circeVersion,
      "io.circe"       %% "circe-refined" % "0.14.5",
      "eu.timepit"     %% "refined"       % "0.11.2"
    )
  )
lazy val betService = (project in file("bet-service"))
  .dependsOn(contracts, betServiceGeneratedServer)
  .aggregate(betServiceGeneratedServer)
  .settings(
    name := "bet-service",
    Compile / mainClass := Some("bet_service.Main"),
    Compile / run / fork := true,
    libraryDependencies ++= commonDeps,
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "versions", _, "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case "reference.conf"   => MergeStrategy.concat
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
lazy val tradingServiceGeneratedServer = (project in file("trading-service/generated/server"))
lazy val tradingServiceGeneratedClient = (project in file("trading-service/generated/client"))

lazy val tradingService = (project in file("trading-service"))
  .dependsOn(contracts, tradingServiceGeneratedServer)
  .aggregate(tradingServiceGeneratedServer, tradingServiceGeneratedClient)
  .settings(
    name := "trading-service",
    Compile / mainClass := Some("trading_service.Main"),
    Compile / run / fork := true,
    libraryDependencies ++= commonDeps
  )

lazy val root = (project in file("."))
  .aggregate(contracts, betService, tradingService)
  .settings(
    name := "scalaLearningProject"
  )