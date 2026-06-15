package com.jobsearch.common.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TopicsTest :
    StringSpec({
        "pipeline topic names are stable" {
            Topics.RAW_OFFERS shouldBe "raw-offers"
            Topics.NORMALIZED_OFFERS shouldBe "normalized-offers"
            Topics.OFFER_PUBLISHED shouldBe "offer.published"
        }
    })
