package com.jobsearch.offer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OpenApiConfigTest :
    StringSpec({
        "exposes offer-service REST API metadata" {
            val api = OpenApiConfig().offerServiceOpenApi()

            api.info.title shouldBe "offer-service API"
            api.info.version shouldBe "v1"
            api.info.description shouldContain "Browse and search"
        }
    })
