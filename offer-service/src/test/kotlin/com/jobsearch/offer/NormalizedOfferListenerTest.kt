package com.jobsearch.offer

import com.jobsearch.proto.processing.v1.NormalizedOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NormalizedOfferListenerTest :
    StringSpec({
        "listener parses normalized bytes and upserts the mapped offer" {
            val repository = mockk<OfferRepository>(relaxed = true)
            val listener = NormalizedOfferListener(repository)
            val normalized =
                NormalizedOffer
                    .newBuilder()
                    .setOfferId("sample:offer-1")
                    .setSource("sample")
                    .setExternalId("offer-1")
                    .setTitle("Engineer")
                    .build()

            val upserted = slot<Offer>()
            listener.onMessage(normalized.toByteArray())

            verify(exactly = 1) { repository.upsert(capture(upserted)) }
            upserted.captured.offerId shouldBe "sample:offer-1"
            upserted.captured.title shouldBe "Engineer"
        }
    })
