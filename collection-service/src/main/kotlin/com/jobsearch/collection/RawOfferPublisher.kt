package com.jobsearch.collection

import com.jobsearch.common.domain.OfferIdentity
import com.jobsearch.common.domain.Topics
import com.jobsearch.proto.collection.v1.RawOffer
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Publishes a [RawOffer] to the `raw-offers` topic, keyed by its `offer_id` so a given offer always
 * lands on the same partition. The value is the raw Protobuf bytes (§1.1 uses plain Protobuf serdes;
 * Apicurio registry integration is a later enrichment).
 */
@Component
class RawOfferPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) {
    fun publish(offer: RawOffer): String {
        val offerId = OfferIdentity.offerId(offer.source, offer.externalId)
        kafkaTemplate.send(Topics.RAW_OFFERS, offerId, offer.toByteArray())
        return offerId
    }
}
