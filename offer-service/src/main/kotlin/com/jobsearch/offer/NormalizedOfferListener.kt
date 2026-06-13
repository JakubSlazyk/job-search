package com.jobsearch.offer

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/** Consumes `normalized-offers` and upserts the canonical offer into Postgres. */
@Component
class NormalizedOfferListener(
    private val repository: OfferRepository,
) {
    @KafkaListener(topics = [Topics.NORMALIZED_OFFERS], groupId = "offer-service")
    fun onMessage(payload: ByteArray) {
        val normalized = NormalizedOffer.parseFrom(payload)
        repository.upsert(OfferMapper.toOffer(normalized))
    }
}
