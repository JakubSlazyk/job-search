package com.jobsearch.common.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OfferIdentityTest :
    StringSpec({
        "offerId joins source and externalId with a colon" {
            OfferIdentity.offerId("justjoinit", "abc-123") shouldBe "justjoinit:abc-123"
        }

        "offerId rejects a blank source" {
            shouldThrow<IllegalArgumentException> { OfferIdentity.offerId(" ", "abc-123") }
        }

        "offerId rejects a blank externalId" {
            shouldThrow<IllegalArgumentException> { OfferIdentity.offerId("justjoinit", "") }
        }
    })
