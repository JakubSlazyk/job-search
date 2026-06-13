package com.jobsearch.offer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus

class OfferControllerTest :
    StringSpec({
        val sample =
            Offer(
                offerId = "sample:offer-1",
                source = "sample",
                externalId = "offer-1",
                title = "Engineer",
                company = "ACME",
                url = "https://example.com/1",
                location = "Remote",
                description = "desc",
            )

        "list returns all offers from the repository" {
            val repository = mockk<OfferRepository> { every { findAll() } returns listOf(sample) }

            OfferController(repository).list() shouldBe listOf(sample)
        }

        "byId returns 200 with the offer when found" {
            val repository = mockk<OfferRepository> { every { findById("sample:offer-1") } returns sample }

            val response = OfferController(repository).byId("sample:offer-1")

            response.statusCode shouldBe HttpStatus.OK
            response.body shouldBe sample
        }

        "byId returns 404 when the offer is missing" {
            val repository = mockk<OfferRepository> { every { findById(any()) } returns null }

            OfferController(repository).byId("missing").statusCode shouldBe HttpStatus.NOT_FOUND
        }
    })
