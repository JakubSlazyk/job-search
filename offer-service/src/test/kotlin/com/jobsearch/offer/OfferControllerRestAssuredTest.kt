package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchIndex
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.hamcrest.Matchers.equalTo

private val sample =
    Offer(
        offerId = "sample:1",
        source = "sample",
        externalId = "1",
        title = "Engineer",
        company = "ACME",
        url = "https://example.com/1",
        location = "Remote",
        description = "desc",
        seniority = "SENIOR",
    )

/**
 * REST contract test (§1.6) over the real Spring MVC stack via RestAssured + standalone MockMvc — no
 * server, no application context. Asserts the HTTP surface (status codes, JSON shape) that the
 * direct [OfferControllerTest] cannot.
 */
class OfferControllerRestAssuredTest :
    StringSpec({
        "GET /api/v1/offers serializes hits to JSON" {
            val index =
                mockk<OfferSearchIndex> {
                    every { search(any()) } returns listOf(sample)
                }
            RestAssuredMockMvc.standaloneSetup(OfferController(index))

            RestAssuredMockMvc
                .given()
                .queryParam("query", "kotlin")
                .`when`()
                .get("/api/v1/offers")
                .then()
                .statusCode(200)
                .body("[0].offerId", equalTo("sample:1"))
                .body("[0].company", equalTo("ACME"))
        }

        "GET /api/v1/offers/{id} returns 200 when found and 404 when missing" {
            val index =
                mockk<OfferSearchIndex> {
                    every { findById("sample:1") } returns sample
                    every { findById("missing") } returns null
                }
            RestAssuredMockMvc.standaloneSetup(OfferController(index))

            RestAssuredMockMvc
                .`when`()
                .get("/api/v1/offers/sample:1")
                .then()
                .statusCode(200)
                .body("offerId", equalTo("sample:1"))

            RestAssuredMockMvc
                .`when`()
                .get("/api/v1/offers/missing")
                .then()
                .statusCode(404)
        }
    })
