package com.jobsearch.offer

import com.jobsearch.common.domain.Topics
import com.jobsearch.offer.search.OfferSearchIndex
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Consumes `normalized-offers` and updates both CQRS sides: upserts the canonical row in Postgres
 * (write model) and indexes the offer in OpenSearch (read model).
 */
@Component
class NormalizedOfferListener(
    private val repository: OfferRepository,
    private val searchIndex: OfferSearchIndex,
) {
    @KafkaListener(topics = [Topics.NORMALIZED_OFFERS], groupId = "offer-service")
    fun onMessage(payload: ByteArray) {
        val offer = OfferMapper.toOffer(NormalizedOffer.parseFrom(payload))
        repository.upsert(offer)
        searchIndex.index(offer)
    }
}
