package com.jobsearch.tracker.kafka

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.offer.v1.OfferPublished
import com.jobsearch.tracker.TrackerRepository
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Drives the enrichment consumer over a real Kafka + Postgres: an `offer.published` event upserts the
 * local offer snapshot (so a tracked row gains its display fields), and a redelivered event with the
 * same `event_id` is a no-op (idempotency). Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class OfferPublishedConsumerIT {
    @Autowired
    private lateinit var jdbc: JdbcClient

    @Autowired
    private lateinit var repository: TrackerRepository

    @Test
    fun `consumes offer published to enrich a tracked offer, idempotent on event_id`() {
        // A user is already tracking the offer before any enrichment event arrives (enrich-later).
        jdbc
            .sql("INSERT INTO tracked_offers (subject, offer_id, status) VALUES ('s1', 'sample:enrich', 'SAVED')")
            .update()

        val event =
            OfferPublished
                .newBuilder()
                .setEventId("evt-1")
                .setOfferId("sample:enrich")
                .setTitle("Senior Kotlin Engineer")
                .setCompany("ACME")
                .setUrl("https://example.com/1")
                .build()

        send(event)
        send(event) // redelivery — must not double-apply

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            repository.findApplication("s1", "sample:enrich")?.snapshot?.title shouldBe "Senior Kotlin Engineer"
        }

        val processed =
            jdbc
                .sql("SELECT count(*) FROM processed_events WHERE event_id = 'evt-1'")
                .query(Long::class.javaObjectType)
                .single()
        processed shouldBe 1L
    }

    private fun send(event: OfferPublished) {
        val props =
            mapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            )
        KafkaProducer(props, StringSerializer(), ByteArraySerializer()).use { producer ->
            producer.send(ProducerRecord(Topics.OFFER_PUBLISHED, event.offerId, event.toByteArray())).get()
        }
    }

    @TestConfiguration
    class StubJwtDecoderConfiguration {
        @Bean
        fun jwtDecoder(): JwtDecoder = JwtDecoder { error("not used in this test") }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("tracker")

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }
}
