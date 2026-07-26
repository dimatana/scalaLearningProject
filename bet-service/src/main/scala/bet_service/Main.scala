package bet_service

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fs2.kafka.KafkaProducer
import org.flywaydb.core.Flyway
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import bet_service.generated.server.apis.DefaultApiRoutes
import java.nio.charset.StandardCharsets

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def makeTransactor(config: AppConfig): Resource[IO, HikariTransactor[IO]] =
    for
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = config.dbUrl,
        user = config.dbUser,
        pass = config.dbPassword,
        connectEC = ec
      )
    yield xa

  private def runMigrations(config: AppConfig): IO[Unit] =
    IO.blocking {
      Flyway
        .configure()
        .dataSource(config.dbUrl, config.dbUser, config.dbPassword)
        .load()
        .migrate()
    }.void

  private def readResource(resourcePath: String): IO[String] =
    IO.blocking {
      val stream = Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
        .getOrElse(throw new IllegalStateException(s"Missing classpath resource: $resourcePath"))
      try new String(stream.readAllBytes(), StandardCharsets.UTF_8)
      finally stream.close()
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
  private def routes(repo: BetRepository, producer: KafkaProducer[IO, String, String]): HttpRoutes[IO] =
    DefaultApiRoutes(BetApiDelegate(repo, producer)).routes <+> docsRoutes


  val run: IO[Unit] =
    val config = AppConfig.load()
    (makeTransactor(config), BetEventProducer.make(config.kafkaBrokers)).tupled.use { case (xa, producer) =>
      val repo = BetRepository(xa)
      for
        _ <- runMigrations(config)
        httpApp = CORS.policy.withAllowOriginAll(routes(repo, producer)).orNotFound
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(Port.fromInt(config.httpPort).getOrElse(port"3000"))
          .withHttpApp(httpApp)
          .build
          .useForever
      yield ()
    }