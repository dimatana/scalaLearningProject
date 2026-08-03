package trading_service

import cats.MonoidK.ops.toAllMonoidKOps
import cats.effect.{IO, IOApp, Resource}
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.comcast.ip4s.*
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.{CORS, Logger as RequestLogger}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import trading_service.generated.server.apis.DefaultApiRoutes

import java.nio.charset.StandardCharsets

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def makeTransactor(config: AppConfig): Resource[IO, HikariTransactor[IO]] =
    val hc = new com.zaxxer.hikari.HikariConfig()
    hc.setDriverClassName("org.postgresql.Driver")
    hc.setJdbcUrl(config.dbUrl)
    hc.setUsername(config.dbUser)
    hc.setPassword(config.dbPassword)
    hc.setMaximumPoolSize(config.dbMaxPoolSize)
    hc.setMinimumIdle(config.dbMinIdle)
    hc.setConnectionTimeout(config.dbConnectionTimeout.toMillis)

    HikariTransactor.fromHikariConfig[IO](hc)
      .evalTap(_ => summon[Logger[IO]].info("acquire: db connection pool"))
      .onFinalize(summon[Logger[IO]].info("release: db connection pool"))

  private def runMigrations(config: AppConfig): IO[Unit] =
    IO.blocking {
      Flyway
        .configure()
        .dataSource(config.dbUrl, config.dbUser, config.dbPassword)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    }.void

  private def readResource(resourcePath: String): IO[String] =
    IO.blocking(Option(getClass.getClassLoader.getResourceAsStream(resourcePath)))
      .flatMap {
        case Some(stream) =>
          IO.blocking(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
            .guarantee(IO.blocking(stream.close()))
        case None =>
          IO.raiseError(new IllegalStateException(s"Missing classpath resource: $resourcePath"))
      }

  private val docsRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case GET -> Root / "api-docs" / "openapi.yaml" =>
        readResource("static/openapi.yaml").map(spec =>
          Response[IO](status = Status.Ok)
            .withEntity(spec)(using EntityEncoder.stringEncoder[IO])
            .putHeaders(Header.Raw(org.typelevel.ci.CIString("Content-Type"), "application/yaml; charset=utf-8"))
        )
      case GET -> Root / "swagger-ui" =>
        readResource("static/swagger-ui.html").map(html =>
          Response[IO](status = Status.Ok)
            .withEntity(html)(using EntityEncoder.stringEncoder[IO])
            .putHeaders(Header.Raw(org.typelevel.ci.CIString("Content-Type"), "text/html; charset=utf-8"))
        )

  private def routes(repo: EventRepository): HttpRoutes[IO] =
    DefaultApiRoutes(EventApiDelegate(repo)).routes <+> docsRoutes

  val run: IO[Unit] =
    val config = AppConfig.load()
    makeTransactor(config).use { xa =>
      val repo = EventRepository(xa)
      val consumerStream = BetPlacedConsumer.stream(
        config.kafkaBrokers,
        config.kafkaGroupId,
        config.kafkaTopic,
        repo
      )
      for
        _ <- runMigrations(config)
        httpApp = RequestLogger.httpApp(logHeaders = true, logBody = false)(
          CORS.policy.withAllowOriginAll(routes(repo)).orNotFound
        )
        _ <- (
          consumerStream.compile.drain.background,
          EmberServerBuilder
            .default[IO]
            .withHost(host"0.0.0.0")
            .withPort(Port.fromInt(config.httpPort).getOrElse(port"3001"))
            .withHttpApp(httpApp)
            .build
            .evalTap(srv => summon[Logger[IO]].info(s"acquire: http server bound at ${srv.address}"))
            .onFinalize(summon[Logger[IO]].info("release: http server"))
        ).tupled.useForever
      yield ()
    }
