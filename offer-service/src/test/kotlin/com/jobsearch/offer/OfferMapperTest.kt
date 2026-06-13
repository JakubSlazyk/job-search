package com.jobsearch.offer

import com.jobsearch.proto.processing.v1.NormalizedOffer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OfferMapperTest :
    StringSpec({
        "maps every NormalizedOffer field onto the Offer" {
            val normalized =
                NormalizedOffer
                    .newBuilder()
                    .setOfferId("sample:offer-1")
                    .setSource("sample")
                    .setExternalId("offer-1")
                    .setTitle("Engineer")
                    .setCompany("ACME")
                    .setUrl("https://example.com/1")
                    .setLocation("Remote")
                    .setDescription("desc")
                    .build()

            OfferMapper.toOffer(normalized) shouldBe
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
        }
    })
