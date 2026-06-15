package com.jobsearch.offer.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OfferSearchCriteriaTest :
    StringSpec({
        "passes sensible inputs through and translates page to a zero-based offset" {
            val criteria = OfferSearchCriteria.paged("kotlin", "sample", "Remote", "SENIOR", page = 2, size = 20)

            criteria.query shouldBe "kotlin"
            criteria.source shouldBe "sample"
            criteria.location shouldBe "Remote"
            criteria.seniority shouldBe "SENIOR"
            criteria.size shouldBe 20
            criteria.from shouldBe 40
        }

        "defaults a null query to empty" {
            OfferSearchCriteria.paged(null, null, null, null, page = 0, size = 20).query shouldBe ""
        }

        "clamps an oversized size to MAX_SIZE" {
            OfferSearchCriteria.paged(null, null, null, null, page = 0, size = 99_999).size shouldBe
                OfferSearchCriteria.MAX_SIZE
        }

        "treats a non-positive size as the default" {
            OfferSearchCriteria.paged(null, null, null, null, page = 0, size = 0).size shouldBe
                OfferSearchCriteria.DEFAULT_SIZE
            OfferSearchCriteria.paged(null, null, null, null, page = 0, size = -5).size shouldBe
                OfferSearchCriteria.DEFAULT_SIZE
        }

        "floors a negative page at zero" {
            OfferSearchCriteria.paged(null, null, null, null, page = -1, size = 20).from shouldBe 0
        }

        "caps a deep page so from + size stays within the result window" {
            val criteria = OfferSearchCriteria.paged(null, null, null, null, page = 1_000, size = 100)

            criteria.size shouldBe 100
            criteria.from shouldBe OfferSearchCriteria.MAX_RESULT_WINDOW - 100
        }

        "computes the offset without overflowing for an extreme page" {
            val criteria = OfferSearchCriteria.paged(null, null, null, null, page = Int.MAX_VALUE, size = 100)

            criteria.from shouldBe OfferSearchCriteria.MAX_RESULT_WINDOW - 100
        }
    })
