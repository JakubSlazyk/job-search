package com.jobsearch.processing.adapter.outbound

import com.jobsearch.common.domain.Topics
import com.jobsearch.processing.application.NormalizedOfferSink
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/** Outbound adapter: publishes canonicalized offers to `normalized-offers`, keyed by `offer_id`. */
@Component
class KafkaNormalizedOfferPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) : NormalizedOfferSink {
    override fun send(offer: NormalizedOffer) {
        kafkaTemplate.send(Topics.NORMALIZED_OFFERS, offer.offerId, offer.toByteArray())
    }
}
