package com.jobsearch.offer

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Persists an offer and its `offer.published` outbox event atomically (ADR 0002, §1.5). The upsert
 * and the outbox insert run in one transaction, closing the dual-write gap: either both commit or
 * neither does. OpenSearch indexing is intentionally left to the caller, after this commits.
 */
@Service
class OfferIngestionService(
    private val repository: OfferRepository,
    private val outbox: OutboxRepository,
) {
    @Transactional
    fun ingest(offer: Offer) {
        repository.upsert(offer)
        val event = OfferPublishedFactory.from(offer)
        outbox.append(aggregateId = offer.offerId, type = "OfferPublished", payload = event.toByteArray())
    }
}
