package com.jobsearch.collection

import com.jobsearch.common.domain.Topics
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.kafka.core.KafkaTemplate

class RawOfferPublisherTest :
    StringSpec({
        "publish sends the offer bytes to raw-offers keyed by offer_id" {
            val kafkaTemplate = mockk<KafkaTemplate<String, ByteArray>>(relaxed = true)
            val publisher = RawOfferPublisher(kafkaTemplate)
            val offer = SampleOffers.sample()

            val offerId = publisher.publish(offer)

            offerId shouldBe "sample:offer-1"
            val valueSlot = slot<ByteArray>()
            verify(exactly = 1) { kafkaTemplate.send(Topics.RAW_OFFERS, "sample:offer-1", capture(valueSlot)) }
            valueSlot.captured shouldBe offer.toByteArray()
        }
    })
