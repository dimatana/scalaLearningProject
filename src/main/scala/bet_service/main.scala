package bet_service

import cats.effect.{IO, IOApp}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.http4s.server.middleware.CORS
import org.typelevel.ci.CIString
import ping.server.models.PingResponse

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.time.Instant

object Main extends IOApp.Simple {
  private val specPath: Path = Paths.get("openapi.yaml")

  private def readSpec: IO[String] =
    IO.blocking(Files.readString(specPath, StandardCharsets.UTF_8))

  private def readSwaggerHtml: IO[String] =
    IO.blocking {
      val stream = Option(getClass.getClassLoader.getResourceAsStream("bet_service/swagger-ui.html"))
        .getOrElse(throw new IllegalStateException("Missing resource: bet_service/swagger-ui.html"))
      try new String(stream.readAllBytes(), StandardCharsets.UTF_8)
      finally stream.close()
    }

  private val httpRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "ping" =>
      Ok(PingResponse(status = "pong", timestamp = Instant.now()))

    case GET -> Root / "api-docs" / "openapi.yaml" =>
      readSpec.map(spec =>
        Response[IO](status = Status.Ok)
          .withEntity(spec)(using EntityEncoder.stringEncoder[IO])
          .putHeaders(Header.Raw(CIString("Content-Type"), "application/yaml; charset=utf-8"))
      )

    case GET -> Root / "swagger-ui" =>
      readSwaggerHtml.map(html =>
        Response[IO](status = Status.Ok)
          .withEntity(html)(using EntityEncoder.stringEncoder[IO])
          .putHeaders(Header.Raw(CIString("Content-Type"), "text/html; charset=utf-8"))
      )
  }

  private val routes: HttpApp[IO] =
    CORS.policy.withAllowOriginAll(httpRoutes).orNotFound

  val run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"3000")
      .withHttpApp(routes)
      .build
      .useForever
}