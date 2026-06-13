package com.jobsearch.processing

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.collection.v1.RawOffer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/** Consumes `raw-offers`, normalizes, and re-publishes to `normalized-offers`. */
@Component
class RawOfferListener(
    private val publisher: NormalizedOfferPublisher,
) {
    @KafkaListener(topics = [Topics.RAW_OFFERS], groupId = "processing-service")
    fun onMessage(payload: ByteArray) {
        val raw = RawOffer.parseFrom(payload)
        publisher.publish(OfferNormalizer.normalize(raw))
    }
}
