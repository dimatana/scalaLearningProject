package trading_service

import cats.effect.IO
import fs2.Stream
import fs2.kafka.*
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

object BetPlacedConsumer:

  private def consumerSettings(
    brokers: String,
    groupId: String
  ): ConsumerSettings[IO, Option[String], Option[Array[Byte]]] =
    ConsumerSettings(
      keyDeserializer = Deserializer[IO, String].option,
      valueDeserializer = Deserializer[IO, Array[Byte]].option
    )
      .withBootstrapServers(brokers)
      .withGroupId(groupId)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(false)

  def stream(
    brokers: String,
    groupId: String,
    topic: String,
    repo: EventRepository
  )(using logger: Logger[IO]): Stream[IO, Unit] =
    runOnce(brokers, groupId, topic, repo)
      .handleErrorWith { err =>
        Stream.eval(logger.error(err)("Consumer stream crashed, restarting in 5s")) ++
          Stream.sleep[IO](5.seconds) ++
          stream(brokers, groupId, topic, repo) // relansează recursiv, nu se mai oprește definitiv
      }

  private def runOnce(
    brokers: String,
    groupId: String,
    topic: String,
    repo: EventRepository
  )(using logger: Logger[IO]): Stream[IO, Unit] =
    KafkaConsumer
      .stream(consumerSettings(brokers, groupId))
      .subscribeTo(topic)
      .records
      .evalMap { committable =>
        handleRecord(committable.record.value, repo)
          .handleErrorWith(err =>
            logger.warn(err)(s"unhandled error processing record, key=${committable.record.key}")
          ) *> committable.offset.commit
      }
      .evalTap(_ => logger.info("acquire: kafka consumer stream started"))
      .onFinalize(logger.info("release: kafka consumer stream stopped"))

  private def handleRecord(payload: Option[Array[Byte]], repo: EventRepository)(using logger: Logger[IO]): IO[Unit] =
    RecordDecoder.decodeRecord(payload) match
      case Left(DecodeError.MissingPayload) =>
        logger.warn("Skipping record with missing payload")

      case Left(DecodeError.EmptyPayload) =>
        logger.warn("Skipping empty payload record")

      case Left(DecodeError.InvalidJson(cause)) =>
        logger.warn(s"Skipping malformed JSON: ${cause.getMessage}")

      case Right(betPlaced) =>
        for
          result <- repo.incrementIfNotProcessed(betPlaced.betId, betPlaced.eventId)
          _ <- result match
            case Right(ProcessResult.Counted) =>
              logger.info(s"Counted bet ${betPlaced.betId} for event ${betPlaced.eventId}")

            case Right(ProcessResult.AlreadyProcessed) =>
              logger.warn(s"Duplicate bet ${betPlaced.betId} — already processed, skipping")

            case Left(EventError.NotFound(id)) =>
              logger.warn(s"Unknown eventId=$id for bet ${betPlaced.betId}, skipping")

            case Left(err) =>
              logger.warn(s"Failed to process bet ${betPlaced.betId}: ${err.message}")
        yield ()
