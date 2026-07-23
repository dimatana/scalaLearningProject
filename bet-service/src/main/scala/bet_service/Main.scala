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
import java.nio.file.{Files, Path, Paths}

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

  private def errorResponse(err: BetError): IO[Response[IO]] =
    err match
      case BetError.NotFound(_)          => NotFound(ErrorResponse(err.message))
      case BetError.InvalidStake(_)      => UnprocessableEntity(ErrorResponse(err.message))
      case BetError.InvalidOdds(_)       => UnprocessableEntity(ErrorResponse(err.message))
      case BetError.RepositoryFailure(_) => InternalServerError(ErrorResponse(err.message))

  private def resolveExistingPath(fileName: String): IO[Path] =
    IO.blocking {
      val candidates = List(
        Paths.get("bet-service", fileName),
        Paths.get(fileName)
      )
      candidates.find(Files.exists(_)).getOrElse {
        throw new IllegalStateException(
          s"Missing file '$fileName'. Tried: ${candidates.map(_.toString).mkString(", ")}"
        )
      }
    }

  private def readTextFile(path: Path): IO[String] =
    IO.blocking(Files.readString(path, StandardCharsets.UTF_8))

  private def readSpec: IO[String] =
    resolveExistingPath("openapi.yaml").flatMap(readTextFile)

  private def readSwaggerHtml: IO[String] =
    resolveExistingPath("swagger-ui.html").flatMap(readTextFile)

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