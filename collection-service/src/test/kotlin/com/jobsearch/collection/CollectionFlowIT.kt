package com.jobsearch.collection

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.collection.v1.RawOffer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Verifies the emit path: POST /api/v1/collect publishes a RawOffer to `raw-offers`.
 * Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionFlowIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @Test
    fun `collect endpoint emits a RawOffer to raw-offers`() {
        val consumer = consumerFactory().createConsumer("collection-it", "")
        consumer.subscribe(listOf(Topics.RAW_OFFERS))

        HttpClient.newHttpClient().send(
            HttpRequest
                .newBuilder(URI.create("http://localhost:$port/api/v1/collect"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.discarding(),
        )

        val record = KafkaTestUtils.getSingleRecord(consumer, Topics.RAW_OFFERS, Duration.ofSeconds(10))
        consumer.close()

        assertEquals("sample:offer-1", record.key())
        assertEquals("Senior Kotlin Engineer", RawOffer.parseFrom(record.value()).title)
    }

    private fun consumerFactory(): ConsumerFactory<String, ByteArray> =
        DefaultKafkaConsumerFactory(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ),
            StringDeserializer(),
            ByteArrayDeserializer(),
        )

    companion object {
        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))

        @JvmStatic
        @DynamicPropertySource
        fun kafkaProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }
}
