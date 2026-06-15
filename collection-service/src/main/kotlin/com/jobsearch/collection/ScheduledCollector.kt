package com.jobsearch.collection

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Polls the sources on a fixed delay. Disabled by default — enable with
 * `collection.polling.enabled=true` (interval via `collection.polling.interval-ms`).
 */
@Component
@ConditionalOnProperty(prefix = "collection.polling", name = ["enabled"], havingValue = "true")
class ScheduledCollector(
    private val collector: OfferCollector,
) {
    @Scheduled(fixedDelayString = "\${collection.polling.interval-ms:60000}")
    fun poll() {
        collector.collectAll().subscribe()
    }
}
