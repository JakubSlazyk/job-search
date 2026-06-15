package com.jobsearch.collection.source

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SampleOfferSourceTest :
    StringSpec({
        "sample source emits one uniform RawOffer" {
            val offers = SampleOfferSource().fetch().collectList().block()!!

            offers.size shouldBe 1
            offers[0].source shouldBe "sample"
            offers[0].externalId shouldBe "offer-1"
            offers[0].title shouldBe "Senior Kotlin Engineer"
            offers[0].rawContent.isEmpty shouldBe false
        }
    })
