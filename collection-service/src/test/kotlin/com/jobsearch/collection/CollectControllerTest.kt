package com.jobsearch.collection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

class CollectControllerTest :
    StringSpec({
        "collect triggers a run and returns 202 with the published count" {
            val collector = mockk<OfferCollector> { every { collectAll() } returns Mono.just(2) }

            val response = CollectController(collector).collect().block()!!

            response.statusCode shouldBe HttpStatus.ACCEPTED
            response.body shouldBe mapOf("collected" to 2)
        }
    })
