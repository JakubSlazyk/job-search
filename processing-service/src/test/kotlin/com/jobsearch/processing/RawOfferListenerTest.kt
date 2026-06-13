package com.jobsearch.processing

import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.NormalizedOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class RawOfferListenerTest :
    StringSpec({
        "listener parses raw bytes, normalizes, and publishes" {
            val publisher = mockk<NormalizedOfferPublisher>(relaxed = true)
            val listener = RawOfferListener(publisher)
            val raw =
                RawOffer
                    .newBuilder()
                    .setSource("sample")
                    .setExternalId("offer-1")
                    .setTitle("Engineer")
                    .build()

            val published = slot<NormalizedOffer>()
            listener.onMessage(raw.toByteArray())

            verify(exactly = 1) { publisher.publish(capture(published)) }
            published.captured.offerId shouldBe "sample:offer-1"
            published.captured.title shouldBe "Engineer"
        }
    })
