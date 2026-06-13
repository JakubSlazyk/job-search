package com.jobsearch.processing.adapter.inbound

import com.jobsearch.common.domain.Topics
import com.jobsearch.processing.application.OfferProcessor
import com.jobsearch.proto.collection.v1.RawOffer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/** Inbound adapter: consumes `raw-offers` and drives the [OfferProcessor]. */
@Component
class RawOfferKafkaListener(
    private val processor: OfferProcessor,
) {
    @KafkaListener(topics = [Topics.RAW_OFFERS], groupId = "processing-service")
    fun onMessage(payload: ByteArray) {
        processor.process(RawOffer.parseFrom(payload))
    }
}
