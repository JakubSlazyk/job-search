package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.http.HttpStatus

private val sample =
    Offer(
        offerId = "sample:offer-1",
        source = "sample",
        externalId = "offer-1",
        title = "Engineer",
        company = "ACME",
        url = "https://example.com/1",
        location = "Remote",
        description = "desc",
        seniority = "SENIOR",
    )

class OfferControllerTest :
    StringSpec({
        "search maps query params to criteria and returns hits" {
            val index = mockk<OfferSearchIndex>()
            val criteria = slot<OfferSearchCriteria>()
            every { index.search(capture(criteria)) } returns listOf(sample)

            val result =
                OfferController(index).search(
                    query = "kotlin",
                    source = "sample",
                    location = null,
                    seniority = "SENIOR",
                    page = 2,
                    size = 10,
                )

            result shouldBe listOf(sample)
            criteria.captured.query shouldBe "kotlin"
            criteria.captured.source shouldBe "sample"
            criteria.captured.seniority shouldBe "SENIOR"
            criteria.captured.from shouldBe 20 // page 2 * size 10
            criteria.captured.size shouldBe 10
        }

        "byId returns 200 when found and 404 when missing" {
            val index =
                mockk<OfferSearchIndex> {
                    every { findById("sample:offer-1") } returns sample
                    every { findById("missing") } returns null
                }

            OfferController(index).byId("sample:offer-1").let {
                it.statusCode shouldBe HttpStatus.OK
                it.body shouldBe sample
            }
            OfferController(index).byId("missing").statusCode shouldBe HttpStatus.NOT_FOUND
        }
    })
