package com.jobsearch.offer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.Instant

class OfferPublishedFactoryTest :
    StringSpec({
        val offer =
            Offer("sample:1", "sample", "1", "Engineer", "ACME", "https://example.com/1", "Remote", "desc", "SENIOR")

        "maps offer fields and the published_at timestamp" {
            val at = Instant.parse("2026-06-14T10:15:30.000000123Z")
            val event = OfferPublishedFactory.from(offer, publishedAt = at, eventId = "fixed-id")

            event.eventId shouldBe "fixed-id"
            event.offerId shouldBe "sample:1"
            event.title shouldBe "Engineer"
            event.company shouldBe "ACME"
            event.url shouldBe "https://example.com/1"
            event.publishedAt.seconds shouldBe at.epochSecond
            event.publishedAt.nanos shouldBe at.nano
        }

        "generates a non-blank event id by default" {
            OfferPublishedFactory.from(offer).eventId.shouldNotBeBlank()
        }

        "derives the same default event id for identical offer content (idempotent on redelivery)" {
            val first = OfferPublishedFactory.from(offer).eventId
            val second = OfferPublishedFactory.from(offer.copy()).eventId
            first shouldBe second
        }

        "derives a different default event id when any offer field changes" {
            val baseline = OfferPublishedFactory.from(offer).eventId
            OfferPublishedFactory.from(offer.copy(title = "Staff Engineer")).eventId shouldNotBe baseline
            OfferPublishedFactory.from(offer.copy(description = "updated")).eventId shouldNotBe baseline
            OfferPublishedFactory.from(offer.copy(seniority = "LEAD")).eventId shouldNotBe baseline
        }
    })
