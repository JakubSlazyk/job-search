package com.jobsearch.tracker.kafka

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.offer.v1.OfferPublished
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/** Inbound adapter: consumes `offer.published` and updates the local offer snapshot (enrichment). */
@Component
class OfferPublishedListener(
    private val enrichmentService: EnrichmentService,
) {
    @KafkaListener(topics = [Topics.OFFER_PUBLISHED], groupId = "tracker-service")
    fun onMessage(payload: ByteArray) {
        enrichmentService.apply(OfferPublished.parseFrom(payload))
    }
}
