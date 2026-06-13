package com.jobsearch.processing

import com.jobsearch.proto.collection.v1.RawOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OfferNormalizerTest :
    StringSpec({
        "normalize stamps the offer_id and trims whitespace" {
            val raw =
                RawOffer
                    .newBuilder()
                    .setSource("justjoinit")
                    .setExternalId("abc-123")
                    .setTitle("  Kotlin Dev  ")
                    .setCompany("  ACME  ")
                    .setLocation("  Remote ")
                    .build()

            val normalized = OfferNormalizer.normalize(raw)

            normalized.offerId shouldBe "justjoinit:abc-123"
            normalized.title shouldBe "Kotlin Dev"
            normalized.company shouldBe "ACME"
            normalized.location shouldBe "Remote"
        }
    })
