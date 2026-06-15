package com.jobsearch.offer

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.offer.v1.OfferPublished
import io.debezium.testing.testcontainers.ConnectorConfiguration
import io.debezium.testing.testcontainers.DebeziumContainer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID

/**
 * Full §1.5 CDC path: an ingested offer's outbox row is captured by Debezium (Outbox Event Router)
 * and emitted to `offer.published` as raw [OfferPublished] protobuf bytes — the same wire format the
 * other topics use. Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class OfferPublishedCdcIT {
    @Test
    fun `ingested offer is published to offer-published as OfferPublished protobuf`() {
        service.ingest(
            Offer(
                "sample:cdc",
                "sample",
                "cdc",
                "Engineer",
                "ACME",
                "https://example.com/cdc",
                "Remote",
                "d",
                "SENIOR",
            ),
        )

        kafkaConsumer().use { consumer ->
            consumer.subscribe(listOf(Topics.OFFER_PUBLISHED))
            val record =
                pollUntil(consumer) { it.key() == "sample:cdc" }
                    ?: error("no offer.published record for sample:cdc within timeout")

            val event = OfferPublished.parseFrom(record.value())
            assertEquals("sample:cdc", event.offerId)
            assertEquals("Engineer", event.title)
            assertTrue(event.eventId.isNotBlank())
        }
    }

    private fun pollUntil(
        consumer: KafkaConsumer<String, ByteArray>,
        predicate: (org.apache.kafka.clients.consumer.ConsumerRecord<String, ByteArray>) -> Boolean,
    ): org.apache.kafka.clients.consumer.ConsumerRecord<String, ByteArray>? {
        val deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis()
        while (System.currentTimeMillis() < deadline) {
            for (record in consumer.poll(Duration.ofMillis(500))) {
                if (predicate(record)) return record
            }
        }
        return null
    }

    companion object {
        private val network = Network.newNetwork()

        @Container
        @JvmStatic
        val kafka: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
                .withNetwork(network)
                .withNetworkAliases("kafka")

        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withCommand(
                    "postgres",
                    "-c",
                    "wal_level=logical",
                    "-c",
                    "max_wal_senders=10",
                    "-c",
                    "max_replication_slots=10",
                )

        @Container
        @JvmStatic
        val debezium: DebeziumContainer =
            DebeziumContainer(DockerImageName.parse("quay.io/debezium/connect:3.0.0.Final"))
                .withNetwork(network)
                .withKafka(kafka)
                .dependsOn(kafka, postgres)

        private lateinit var service: OfferIngestionService

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            Flyway
                .configure()
                .dataSource(dataSource)
                .load()
                .migrate()
            val jdbc = JdbcClient.create(dataSource)
            service = OfferIngestionService(OfferRepository(jdbc), OutboxRepository(jdbc))

            val connector =
                ConnectorConfiguration
                    .forJdbcContainer(postgres)
                    .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                    .with("plugin.name", "pgoutput")
                    .with("topic.prefix", "offer-service")
                    .with("slot.name", "offer_outbox_slot")
                    .with("table.include.list", "public.outbox")
                    .with("tombstones.on.delete", "false")
                    .with("transforms", "outbox")
                    .with("transforms.outbox.type", "io.debezium.transforms.outbox.EventRouter")
                    .with("transforms.outbox.table.field.event.key", "aggregateid")
                    .with("transforms.outbox.route.by.field", "aggregatetype")
                    .with("transforms.outbox.route.topic.replacement", Topics.OFFER_PUBLISHED)
                    .with("key.converter", "org.apache.kafka.connect.storage.StringConverter")
                    .with("value.converter", "io.debezium.converters.BinaryDataConverter")
            debezium.registerConnector("offer-outbox-connector", connector)
        }

        private fun kafkaConsumer(): KafkaConsumer<String, ByteArray> =
            KafkaConsumer(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG to "cdc-it-${UUID.randomUUID()}",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ),
                StringDeserializer(),
                ByteArrayDeserializer(),
            )
    }
}
