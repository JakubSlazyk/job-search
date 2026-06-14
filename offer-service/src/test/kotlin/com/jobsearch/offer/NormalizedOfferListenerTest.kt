package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchIndex
import com.jobsearch.proto.processing.v1.NormalizedOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NormalizedOfferListenerTest :
    StringSpec({
        "listener upserts the write model and indexes the read model" {
            val repository = mockk<OfferRepository>(relaxed = true)
            val searchIndex = mockk<OfferSearchIndex>(relaxed = true)
            val listener = NormalizedOfferListener(repository, searchIndex)
            val normalized =
                NormalizedOffer
                    .newBuilder()
                    .setOfferId("sample:offer-1")
                    .setSource("sample")
                    .setExternalId("offer-1")
                    .setTitle("Engineer")
                    .build()

            val upserted = slot<Offer>()
            val indexed = slot<Offer>()
            listener.onMessage(normalized.toByteArray())

            verify(exactly = 1) { repository.upsert(capture(upserted)) }
            verify(exactly = 1) { searchIndex.index(capture(indexed)) }
            upserted.captured.offerId shouldBe "sample:offer-1"
            indexed.captured.offerId shouldBe "sample:offer-1"
        }
    })
