package com.jobsearch.offer

import com.jobsearch.common.domain.Topics
import com.jobsearch.offer.search.OfferSearchIndex
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Consumes `normalized-offers` and updates both CQRS sides: transactionally upserts the canonical
 * row and its `offer.published` outbox event in Postgres (write model, §1.5), then indexes the offer
 * in OpenSearch (read model) after that commit.
 */
@Component
class NormalizedOfferListener(
    private val ingestionService: OfferIngestionService,
    private val searchIndex: OfferSearchIndex,
) {
    @KafkaListener(topics = [Topics.NORMALIZED_OFFERS], groupId = "offer-service")
    fun onMessage(payload: ByteArray) {
        val offer = OfferMapper.toOffer(NormalizedOffer.parseFrom(payload))
        ingestionService.ingest(offer)
        searchIndex.index(offer)
    }
}
