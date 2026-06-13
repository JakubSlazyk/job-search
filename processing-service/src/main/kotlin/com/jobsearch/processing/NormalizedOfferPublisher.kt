package com.jobsearch.processing

import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.processing.v1.NormalizedOffer
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/** Publishes a [NormalizedOffer] to `normalized-offers`, keyed by `offer_id`. */
@Component
class NormalizedOfferPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) {
    fun publish(offer: NormalizedOffer) {
        kafkaTemplate.send(Topics.NORMALIZED_OFFERS, offer.offerId, offer.toByteArray())
    }
}
