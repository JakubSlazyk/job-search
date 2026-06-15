package com.jobsearch.offer

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/**
 * Writes rows to the transactional outbox (ADR 0002). Shares the [JdbcClient]/`DataSource` with
 * [OfferRepository], so an [append] called inside the same transaction commits atomically with the
 * offer upsert. Debezium drains the rows to `offer.published`; the app never writes Kafka directly.
 */
@Repository
class OutboxRepository(
    private val jdbc: JdbcClient,
) {
    fun append(
        aggregateId: String,
        type: String,
        payload: ByteArray,
        aggregateType: String = "offer",
    ) {
        jdbc
            .sql(
                """
                INSERT INTO outbox (aggregatetype, aggregateid, type, payload)
                VALUES (:aggregateType, :aggregateId, :type, :payload)
                """.trimIndent(),
            ).param("aggregateType", aggregateType)
            .param("aggregateId", aggregateId)
            .param("type", type)
            .param("payload", payload)
            .update()
    }

    /** Removes already-drained rows older than [cutoffEpochMillis]; returns the number deleted. */
    fun deleteOlderThan(cutoffEpochMillis: Long): Int =
        jdbc
            .sql("DELETE FROM outbox WHERE created_at < to_timestamp(:cutoff / 1000.0)")
            .param("cutoff", cutoffEpochMillis)
            .update()
}
