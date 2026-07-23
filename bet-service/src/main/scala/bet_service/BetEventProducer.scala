package bet_service

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import contracts.BetPlaced
import fs2.kafka.*
import io.circe.syntax.*
import java.time.Instant

object BetEventProducer:

  private def producerSettings(brokers: String): ProducerSettings[IO, String, String] =
    ProducerSettings[IO, String, String]
      .withBootstrapServers(brokers)

  def make(brokers: String): Resource[IO, KafkaProducer[IO, String, String]] =
    KafkaProducer[IO].resource(producerSettings(brokers)) // KafkaProducer[IO].resource, nu KafkaProducer.resource — necesită tipul efectului explicit

  def publish(producer: KafkaProducer[IO, String, String], bet: Bet): IO[Unit] =
    val event = BetPlaced(
      betId = bet.id,
      eventId = bet.eventId,
      stake = bet.stake,
      odds = bet.odds,
      occurredAt = Instant.now()
    )
    val record = ProducerRecords.one(
      ProducerRecord("bet-placed", bet.id.toString, event.asJson.noSpaces)
    )
    producer.produce(record).flatten.void