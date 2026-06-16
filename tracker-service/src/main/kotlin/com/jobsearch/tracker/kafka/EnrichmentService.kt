package com.jobsearch.tracker.kafka

import com.jobsearch.proto.offer.v1.OfferPublished
import com.jobsearch.tracker.TrackerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Applies an `offer.published` event to the local denormalized copy. The processed-events guard and
 * the snapshot upsert run in one transaction, so a redelivered event (at-least-once) is a no-op —
 * idempotency on `event_id`.
 */
@Service
class EnrichmentService(
    private val repository: TrackerRepository,
) {
    @Transactional
    fun apply(event: OfferPublished) {
        if (!repository.markEventProcessed(event.eventId)) return
        val publishedAt =
            event
                .takeIf { it.hasPublishedAt() }
                ?.publishedAt
                ?.let { Instant.ofEpochSecond(it.seconds, it.nanos.toLong()) }
        repository.upsertSnapshot(
            offerId = event.offerId,
            title = event.title,
            company = event.company,
            url = event.url,
            publishedAt = publishedAt,
        )
    }
}
