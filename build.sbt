import sbt.Keys.testFrameworks

ThisBuild / scalaVersion := "3.8.4"

val http4sVersion = "0.23.30"
val circeVersion  = "0.14.10"

lazy val root = (project in file("."))
  .aggregate(generatedServer, generatedClient)
  .settings(
    name := "scalaLearningProject",
    Compile / mainClass := Some("bet_service.Main"),
    Compile / run / fork := true,
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-effect"         % "3.5.7",
      "org.http4s"     %% "http4s-ember-server" % http4sVersion,
      "org.http4s"     %% "http4s-dsl"          % http4sVersion,
      "org.http4s"     %% "http4s-circe"        % http4sVersion,
      "io.circe"       %% "circe-generic"       % circeVersion,
      "org.typelevel"  %% "log4cats-slf4j"      % "2.7.0",
      "org.typelevel"  %% "log4cats-core"       % "2.7.0",
      "ch.qos.logback" %  "logback-classic"     % "1.5.16",
      "org.tpolecat"  %% "doobie-core"        % "1.0.0-RC5",
      "org.tpolecat"  %% "doobie-hikari"      % "1.0.0-RC5",
      "org.tpolecat"  %% "doobie-postgres"    % "1.0.0-RC5",
      "org.postgresql" % "postgresql"         % "42.7.3",
      "org.flywaydb"   % "flyway-core"        % "10.15.0",
      "org.flywaydb"   % "flyway-database-postgresql" % "10.15.0",
      "org.scalameta"  %% "munit"               % "1.0.0" % Test,
      "org.typelevel"  %% "munit-cats-effect"   % "2.2.0" % Test
    )
 //       testFrameworks +=new TestFramework("munit.Framework")
  )
  .dependsOn(generatedServer)

lazy val generatedServer = (project in file("generated/server"))
lazy val generatedClient = (project in file("generated/client"))