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
        "listener ingests the write model (upsert + outbox) and indexes the read model" {
            val ingestionService = mockk<OfferIngestionService>(relaxed = true)
            val searchIndex = mockk<OfferSearchIndex>(relaxed = true)
            val listener = NormalizedOfferListener(ingestionService, searchIndex)
            val normalized =
                NormalizedOffer
                    .newBuilder()
                    .setOfferId("sample:offer-1")
                    .setSource("sample")
                    .setExternalId("offer-1")
                    .setTitle("Engineer")
                    .build()

            val ingested = slot<Offer>()
            val indexed = slot<Offer>()
            listener.onMessage(normalized.toByteArray())

            verify(exactly = 1) { ingestionService.ingest(capture(ingested)) }
            verify(exactly = 1) { searchIndex.index(capture(indexed)) }
            ingested.captured.offerId shouldBe "sample:offer-1"
            indexed.captured.offerId shouldBe "sample:offer-1"
        }
    })
