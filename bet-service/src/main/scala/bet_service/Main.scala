package bet_service

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import doobie.hikari.HikariTransactor
import fs2.kafka.KafkaProducer
import org.flywaydb.core.Flyway
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.http4s.server.middleware.{CORS, Logger as RequestLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import bet_service.generated.server.apis.DefaultApiRoutes
import java.nio.charset.StandardCharsets
import contracts.BetPlaced

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def makeTransactor(config: AppConfig): Resource[IO, HikariTransactor[IO]] =
    val hikariConfig =
      val hc = new com.zaxxer.hikari.HikariConfig()
      hc.setDriverClassName("org.postgresql.Driver")
      hc.setJdbcUrl(config.dbUrl)
      hc.setUsername(config.dbUser)
      hc.setPassword(config.dbPassword)
      hc.setMaximumPoolSize(config.dbMaxPoolSize) 
      hc.setMinimumIdle(config.dbMinIdle)
      hc.setConnectionTimeout(config.dbConnectionTimeout.toMillis)
      hc

    HikariTransactor.fromHikariConfig[IO](hikariConfig)
      .evalTap(_ => summon[Logger[IO]].info("acquire: db connection pool"))
      .onFinalize(summon[Logger[IO]].info("release: db connection pool"))

  private def runMigrations(config: AppConfig): IO[Unit] =
    IO.blocking {
      Flyway
        .configure()
        .dataSource(config.dbUrl, config.dbUser, config.dbPassword)
        .load()
        .migrate()
    }.void

  private def readResource(resourcePath: String): IO[String] =
    IO.blocking(Option(getClass.getClassLoader.getResourceAsStream(resourcePath)))
      .flatMap {
        case Some(stream) =>
          IO.blocking(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).guarantee(IO.blocking(stream.close()))
        case None =>
          IO.raiseError(new IllegalStateException(s"Missing classpath resource: $resourcePath"))
      }

  private def readSpec: IO[String]        = readResource("static/openapi.yaml")
  private def readSwaggerHtml: IO[String] = readResource("static/swagger-ui.html")

  private val docsRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case GET -> Root / "api-docs" / "openapi.yaml" =>
        readSpec.map(spec =>
          Response[IO](status = Status.Ok)
            .withEntity(spec)(using EntityEncoder.stringEncoder[IO])
            .putHeaders(Header.Raw(org.typelevel.ci.CIString("Content-Type"), "application/yaml; charset=utf-8"))
        )

      case GET -> Root / "swagger-ui" =>
        readSwaggerHtml.map(html =>
          Response[IO](status = Status.Ok)
            .withEntity(html)(using EntityEncoder.stringEncoder[IO])
            .putHeaders(Header.Raw(org.typelevel.ci.CIString("Content-Type"), "text/html; charset=utf-8"))
        )
  private def routes(repo: BetRepository, producer: KafkaProducer[IO, String, BetPlaced], betPlacedTopic: String): HttpRoutes[IO] =
    DefaultApiRoutes(BetApiDelegate(repo, producer, betPlacedTopic)).routes <+> docsRoutes

  val run: IO[Unit] =
    val config = AppConfig.load()
    (makeTransactor(config), BetEventProducer.make(config.kafkaBrokers)).tupled.use { case (xa, producer) =>
      val repo = BetRepository(xa)
      for
        _ <- runMigrations(config)
        httpApp = RequestLogger.httpApp(logHeaders = true, logBody = false)(
          CORS.policy.withAllowOriginAll(routes(repo, producer, config.betPlacedTopic)).orNotFound
        )
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(Port.fromInt(config.httpPort).getOrElse(port"3000"))
          .withHttpApp(httpApp)
          .build
          .evalTap(srv => summon[Logger[IO]].info(s"acquire: http server bound at ${srv.address}"))
          .onFinalize(summon[Logger[IO]].info("release: http server"))
          .useForever
      yield ()
    }