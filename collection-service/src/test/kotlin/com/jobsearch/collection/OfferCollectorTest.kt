package com.jobsearch.collection

import com.jobsearch.collection.source.OfferSource
import com.jobsearch.proto.collection.v1.RawOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux

private fun rawOffer(id: String): RawOffer =
    RawOffer
        .newBuilder()
        .setSource("test")
        .setExternalId(id)
        .build()

class OfferCollectorTest :
    StringSpec({
        "collects from every source, publishes each, and returns the count" {
            val s1 = mockk<OfferSource> { every { fetch() } returns Flux.just(rawOffer("a"), rawOffer("b")) }
            val s2 = mockk<OfferSource> { every { fetch() } returns Flux.just(rawOffer("c")) }
            val publisher = mockk<RawOfferPublisher>(relaxed = true)

            val count = OfferCollector(listOf(s1, s2), publisher).collectAll().block()

            count shouldBe 3
            verify(exactly = 3) { publisher.publish(any()) }
        }

        "a failing source is isolated and does not abort the run" {
            val good = mockk<OfferSource> { every { fetch() } returns Flux.just(rawOffer("a")) }
            val bad = mockk<OfferSource> { every { fetch() } returns Flux.error(RuntimeException("boom")) }
            val publisher = mockk<RawOfferPublisher>(relaxed = true)

            OfferCollector(listOf(bad, good), publisher).collectAll().block() shouldBe 1
        }
    })
