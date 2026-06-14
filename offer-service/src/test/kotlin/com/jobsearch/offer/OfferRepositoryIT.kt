package com.jobsearch.offer

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
 * Verifies the Postgres write model directly (no Spring context): Flyway migrations apply and the
 * upsert persists every column and is idempotent on `offer_id`. Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class OfferRepositoryIT {
    @Test
    fun `upsert persists all columns and is idempotent on offer_id`() {
        val offer =
            Offer("sample:1", "sample", "1", "Engineer", "ACME", "https://example.com/1", "Remote", "desc", "SENIOR")
        repository.upsert(offer)
        repository.upsert(offer.copy(title = "Senior Engineer")) // same id -> update, not insert

        assertEquals(1L, count("sample:1"))
        assertEquals("Senior Engineer", column("title", "sample:1"))
        assertEquals("SENIOR", column("seniority", "sample:1"))
    }

    private fun count(offerId: String): Long =
        jdbc
            .sql("SELECT count(*) FROM offers WHERE offer_id = :id")
            .param("id", offerId)
            .query(Long::class.java)
            .single()

    private fun column(
        name: String,
        offerId: String,
    ): String =
        jdbc
            .sql("SELECT $name FROM offers WHERE offer_id = :id")
            .param("id", offerId)
            .query(String::class.java)
            .single()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))

        private lateinit var jdbc: JdbcClient
        private lateinit var repository: OfferRepository

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
            repository = OfferRepository(jdbc)
        }
    }
}
