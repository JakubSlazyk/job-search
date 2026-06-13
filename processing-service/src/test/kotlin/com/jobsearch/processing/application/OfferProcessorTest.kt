package com.jobsearch.processing.application

import com.jobsearch.processing.domain.OfferCanonicalizer
import com.jobsearch.processing.domain.OfferDeduplicator
import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.NormalizedOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

private fun raw(externalId: String): RawOffer =
    RawOffer
        .newBuilder()
        .setSource("sample")
        .setExternalId(externalId)
        .setTitle("Senior Engineer")
        .build()

class OfferProcessorTest :
    StringSpec({
        "canonicalizes and emits a first-seen offer" {
            val sink = mockk<NormalizedOfferSink>(relaxed = true)
            val processor = OfferProcessor(OfferCanonicalizer(), OfferDeduplicator(), sink)

            val emitted = processor.process(raw("offer-1"))

            emitted shouldBe true
            val sent = slot<NormalizedOffer>()
            verify(exactly = 1) { sink.send(capture(sent)) }
            sent.captured.offerId shouldBe "sample:offer-1"
        }

        "drops a duplicate offer without emitting" {
            val sink = mockk<NormalizedOfferSink>(relaxed = true)
            val processor = OfferProcessor(OfferCanonicalizer(), OfferDeduplicator(), sink)

            processor.process(raw("offer-1")) shouldBe true
            processor.process(raw("offer-1")) shouldBe false

            verify(exactly = 1) { sink.send(any()) }
        }
    })
