package com.jobsearch.notification.kafka

import com.jobsearch.common.domain.Topics
import com.jobsearch.notification.NotificationRepository
import com.jobsearch.notification.grpc.UserContactClient
import com.jobsearch.notification.registerPostgres
import com.jobsearch.proto.offer.v1.OfferPublished
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Drives the reactor-kafka consumer over a real Kafka + Postgres: an `offer.published` event matching a
 * user's criterion produces exactly one delivery, and a redelivered event with the same `event_id` is a
 * no-op (idempotency). The user.v1 gRPC client is stubbed to emit no contact (skips email), keeping the
 * test off the network. Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class OfferPublishedConsumerIT {
    @Autowired
    private lateinit var repository: NotificationRepository

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Test
    fun `consumes a matching offer once, idempotent on event_id`() {
        repository.addCriterion("s1", "kotlin").block()

        val event =
            OfferPublished
                .newBuilder()
                .setEventId("evt-1")
                .setOfferId("sample:1")
                .setTitle("Senior Kotlin Engineer")
                .setCompany("ACME")
                .setUrl("https://example.com/1")
                .build()

        send(event)
        send(event) // redelivery — must not double-deliver

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            val deliveries =
                repository
                    .listDeliveries("s1")
                    .collectList()
                    .block()
                    .orEmpty()
            deliveries.size shouldBe 1
            deliveries.first().offerId shouldBe "sample:1"
        }

        val processed =
            databaseClient
                .sql("SELECT count(*) AS c FROM processed_events WHERE event_id = 'evt-1'")
                .map { row -> row.get("c", Long::class.javaObjectType)!! }
                .one()
                .block()
        processed shouldBe 1L
    }

    private fun send(event: OfferPublished) {
        val props = mapOf<String, Any>(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers)
        KafkaProducer(props, StringSerializer(), ByteArraySerializer()).use { producer ->
            producer.send(ProducerRecord(Topics.OFFER_PUBLISHED, event.offerId, event.toByteArray())).get()
        }
    }

    @TestConfiguration
    class StubBeans {
        /** No Keycloak in tests; the consumer path needs no JWTs, but the resource server wants a decoder. */
        @Bean
        fun reactiveJwtDecoder(): ReactiveJwtDecoder =
            ReactiveJwtDecoder { Mono.error(IllegalStateException("not used")) }

        /** Skip the user.v1 gRPC call (and email) — this test asserts matching + idempotency only. */
        @Bean
        @Primary
        fun stubUserContactClient(): UserContactClient = mockk { every { resolve(any()) } returns Mono.empty() }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("notification")

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.registerPostgres(postgres)
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }
}
