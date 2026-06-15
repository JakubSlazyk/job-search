package com.jobsearch.processing

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Verifies the transform path: a RawOffer on `raw-offers` produces a NormalizedOffer on
 * `normalized-offers`. Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ProcessingFlowIT {
    @Test
    fun `raw offer is normalized and re-published`() {
        val producer =
            DefaultKafkaProducerFactory(
                mapOf<String, Any>(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers),
                StringSerializer(),
                ByteArraySerializer(),
            ).createProducer()
        val consumer =
            DefaultKafkaConsumerFactory(
                mapOf<String, Any>(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ),
                StringDeserializer(),
                ByteArrayDeserializer(),
            ).createConsumer("processing-it", "")
        consumer.subscribe(listOf(Topics.NORMALIZED_OFFERS))

        val raw =
            RawOffer
                .newBuilder()
                .setSource("sample")
                .setExternalId("offer-1")
                .setTitle("Engineer")
                .build()
        producer.send(ProducerRecord(Topics.RAW_OFFERS, "sample:offer-1", raw.toByteArray()))
        producer.flush()

        val record = KafkaTestUtils.getSingleRecord(consumer, Topics.NORMALIZED_OFFERS, Duration.ofSeconds(15))
        consumer.close()
        producer.close()

        val normalized = NormalizedOffer.parseFrom(record.value())
        assertEquals("sample:offer-1", normalized.offerId)
        assertEquals("Engineer", normalized.title)
    }

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
