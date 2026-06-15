package com.jobsearch.offer

import com.jobsearch.proto.offer.v1.OfferPublished
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class OfferIngestionServiceTest :
    StringSpec({
        "ingest upserts the offer and appends a matching OfferPublished outbox event" {
            val repository = mockk<OfferRepository>(relaxed = true)
            val outbox = mockk<OutboxRepository>(relaxed = true)
            val service = OfferIngestionService(repository, outbox)
            val offer =
                Offer(
                    "sample:1",
                    "sample",
                    "1",
                    "Engineer",
                    "ACME",
                    "https://example.com/1",
                    "Remote",
                    "desc",
                    "SENIOR",
                )

            val aggregateId = slot<String>()
            val type = slot<String>()
            val payload = slot<ByteArray>()
            service.ingest(offer)

            verify(exactly = 1) { repository.upsert(offer) }
            verify(exactly = 1) {
                outbox.append(
                    aggregateId = capture(aggregateId),
                    type = capture(type),
                    payload = capture(payload),
                )
            }
            aggregateId.captured shouldBe "sample:1"
            type.captured shouldBe "OfferPublished"

            val event = OfferPublished.parseFrom(payload.captured)
            event.offerId shouldBe "sample:1"
            event.title shouldBe "Engineer"
            event.company shouldBe "ACME"
            event.url shouldBe "https://example.com/1"
            event.eventId.shouldNotBeBlank()
        }
    })
