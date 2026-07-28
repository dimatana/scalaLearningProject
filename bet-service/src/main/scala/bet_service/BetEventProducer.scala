package bet_service

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import contracts.BetPlaced
import fs2.kafka.*
import org.typelevel.log4cats.Logger

import java.time.Instant

object BetEventProducer:

  private def producerSettings(brokers: String): ProducerSettings[IO, String, BetPlaced] =
    ProducerSettings[IO, String, BetPlaced]
      .withBootstrapServers(brokers)

  def make(brokers: String)(using logger: Logger[IO]): Resource[IO, KafkaProducer[IO, String, BetPlaced]] =
    KafkaProducer[IO].resource(producerSettings(brokers))
      .evalTap(_ => logger.info("acquire: kafka producer"))
      .onFinalize(logger.info("release: kafka producer"))


  def publish(producer: KafkaProducer[IO, String, BetPlaced], bet: Bet, topic: String)(using logger: Logger[IO]): IO[Unit] =
    val event = BetPlaced(
      betId = bet.id,
      eventId = bet.eventId,
      stake = bet.stake,
      odds = bet.odds,
      occurredAt = Instant.now()
    )
    val record = ProducerRecords.one(
      ProducerRecord(topic, bet.eventId.toString, event)
    )
    producer.produce(record).flatten.void *>
      logger.info(s"published BetPlaced event: betId=${bet.id}, topic=$topic")