package com.jobsearch.collection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class SampleOffersTest :
    StringSpec({
        "sample offer carries the expected provenance and fields" {
            val offer = SampleOffers.sample(fetchedAt = Instant.ofEpochSecond(1_700_000_000))

            offer.source shouldBe "sample"
            offer.externalId shouldBe "offer-1"
            offer.title shouldBe "Senior Kotlin Engineer"
            offer.fetchedAt.seconds shouldBe 1_700_000_000
            offer.contentType shouldBe "application/json"
            offer.rawContent.isEmpty shouldBe false
        }
    })
