package com.jobsearch.processing.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OfferDeduplicatorTest :
    StringSpec({
        "first sighting is true, repeats within the window are false" {
            val deduplicator = OfferDeduplicator()

            deduplicator.firstSeen("a:1") shouldBe true
            deduplicator.firstSeen("a:1") shouldBe false
            deduplicator.firstSeen("a:2") shouldBe true
        }

        "an id evicted from the bounded window can be seen again" {
            val deduplicator = OfferDeduplicator(capacity = 1)

            deduplicator.firstSeen("a:1") shouldBe true
            deduplicator.firstSeen("a:2") shouldBe true // evicts a:1
            deduplicator.firstSeen("a:1") shouldBe true // a:1 no longer remembered
        }
    })
