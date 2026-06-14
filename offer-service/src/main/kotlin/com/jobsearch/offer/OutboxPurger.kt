package com.jobsearch.offer

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Housekeeping for the transactional outbox (ADR 0002): Debezium captures rows from the WAL, so once
 * they are older than the retention window they can be deleted. Without this the table grows forever.
 */
@Component
class OutboxPurger(
    private val outbox: OutboxRepository,
    @param:Value("\${app.outbox.retention:PT1H}") private val retention: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.outbox.purge-interval-ms:600000}")
    fun purge() {
        val cutoff = System.currentTimeMillis() - retention.toMillis()
        val deleted = outbox.deleteOlderThan(cutoff)
        if (deleted > 0) log.info("Purged {} drained outbox rows older than {}", deleted, retention)
    }
}
