package com.jobsearch.offer

import com.jobsearch.proto.offer.v1.OfferPublished
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

/**
 * Verifies the §1.5 write side against real Postgres: ingesting an offer writes the canonical row
 * AND an outbox row whose payload deserializes to the matching [OfferPublished]. Skipped without
 * Docker. (The DB-WAL hop to `offer.published` is covered by [OfferPublishedCdcIT].)
 */
@Testcontainers(disabledWithoutDocker = true)
class OutboxWriteIT {
    @Test
    fun `ingest writes the offer row and a matching OfferPublished outbox row`() {
        val offer =
            Offer("sample:42", "sample", "42", "Engineer", "ACME", "https://example.com/42", "Remote", "desc", "SENIOR")
        service.ingest(offer)

        assertEquals(1L, count("offers", "sample:42"))
        assertEquals(1L, outboxCount("sample:42"))

        val payload =
            jdbc
                .sql("SELECT payload FROM outbox WHERE aggregateid = :id")
                .param("id", "sample:42")
                .query(ByteArray::class.java)
                .single()
        val event = OfferPublished.parseFrom(payload)
        assertEquals("sample:42", event.offerId)
        assertEquals("Engineer", event.title)
    }

    private fun count(
        table: String,
        offerId: String,
    ): Long =
        jdbc
            .sql("SELECT count(*) FROM $table WHERE offer_id = :id")
            .param("id", offerId)
            .query(Long::class.java)
            .single()

    private fun outboxCount(aggregateId: String): Long =
        jdbc
            .sql("SELECT count(*) FROM outbox WHERE aggregateid = :id")
            .param("id", aggregateId)
            .query(Long::class.java)
            .single()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))

        private lateinit var jdbc: JdbcClient
        private lateinit var service: OfferIngestionService

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val dataSource: DataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            Flyway
                .configure()
                .dataSource(dataSource)
                .load()
                .migrate()
            jdbc = JdbcClient.create(dataSource)
            service = OfferIngestionService(OfferRepository(jdbc), OutboxRepository(jdbc))
        }
    }
}
