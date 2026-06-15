package com.jobsearch.processing.domain

import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.Seniority
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun raw(
    title: String = "Engineer",
    company: String = "ACME",
    location: String = "Remote",
): RawOffer =
    RawOffer
        .newBuilder()
        .setSource("justjoinit")
        .setExternalId("abc-123")
        .setTitle(title)
        .setCompany(company)
        .setLocation(location)
        .build()

class OfferCanonicalizerTest :
    StringSpec({
        val canonicalizer = OfferCanonicalizer()

        "stamps the offer_id and collapses whitespace" {
            val normalized = canonicalizer.canonicalize(raw(title = "  Kotlin   Dev  ", company = "  ACME  "))

            normalized.offerId shouldBe "justjoinit:abc-123"
            normalized.title shouldBe "Kotlin Dev"
            normalized.company shouldBe "ACME"
        }

        "canonicalizes any remote location to \"Remote\"" {
            canonicalizer.canonicalize(raw(location = "Fully Remote (EU)")).location shouldBe "Remote"
            canonicalizer.canonicalize(raw(location = "  Berlin ")).location shouldBe "Berlin"
        }

        "derives seniority from the title" {
            val cases =
                mapOf(
                    "Junior Kotlin Developer" to Seniority.JUNIOR,
                    "Mid Backend Engineer" to Seniority.MID,
                    "Senior Software Engineer" to Seniority.SENIOR,
                    "Lead Platform Engineer" to Seniority.LEAD,
                    "Principal Architect" to Seniority.LEAD,
                    "Backend Engineer" to Seniority.SENIORITY_UNSPECIFIED,
                )
            cases.forEach { (title, expected) ->
                canonicalizer.canonicalize(raw(title = title)).seniority shouldBe expected
            }
        }
    })
