package com.jobsearch.offer

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Verifies the store path: a NormalizedOffer on `normalized-offers` is upserted into Postgres and
 * becomes queryable via REST. Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OfferIngestIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @Test
    fun `normalized offer becomes queryable via REST`() {
        val producer =
            DefaultKafkaProducerFactory(
                mapOf<String, Any>(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers),
                StringSerializer(),
                ByteArraySerializer(),
            ).createProducer()
        val normalized =
            NormalizedOffer
                .newBuilder()
                .setOfferId("sample:offer-1")
                .setSource("sample")
                .setExternalId("offer-1")
                .setTitle("Senior Kotlin Engineer")
                .setCompany("ACME")
                .setUrl("https://example.com/offers/offer-1")
                .setLocation("Remote")
                .setDescription("desc")
                .build()
        producer.send(ProducerRecord(Topics.NORMALIZED_OFFERS, normalized.offerId, normalized.toByteArray()))
        producer.flush()
        producer.close()

        val client = HttpClient.newHttpClient()
        await().atMost(Duration.ofSeconds(20)).pollInterval(500, TimeUnit.MILLISECONDS).untilAsserted {
            val request =
                HttpRequest
                    .newBuilder(URI.create("http://localhost:$port/api/v1/offers/sample:offer-1"))
                    .GET()
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            assertTrue(response.body().contains("Senior Kotlin Engineer"))
        }
    }

    companion object {
        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
