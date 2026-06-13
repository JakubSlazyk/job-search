package com.jobsearch.collection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus

class CollectControllerTest :
    StringSpec({
        "collect endpoint publishes the sample and returns 202 with the offer id" {
            val publisher = mockk<RawOfferPublisher>()
            every { publisher.publish(any()) } returns "sample:offer-1"

            val response = CollectController(publisher).collectSample()

            response.statusCode shouldBe HttpStatus.ACCEPTED
            response.body shouldBe mapOf("offerId" to "sample:offer-1")
        }
    })
