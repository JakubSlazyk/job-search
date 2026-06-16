package com.jobsearch.tracker.kafka

import com.jobsearch.proto.offer.v1.OfferPublished
import com.jobsearch.tracker.TrackerRepository
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class EnrichmentServiceTest :
    StringSpec({
        fun event() =
            OfferPublished
                .newBuilder()
                .setEventId("evt-1")
                .setOfferId("sample:1")
                .setTitle("Engineer")
                .setCompany("ACME")
                .setUrl("https://example.com/1")
                .build()

        "applies the snapshot the first time an event is seen" {
            val repository = mockk<TrackerRepository>()
            every { repository.markEventProcessed("evt-1") } returns true
            every { repository.upsertSnapshot("sample:1", "Engineer", "ACME", "https://example.com/1", null) } just Runs

            EnrichmentService(repository).apply(event())

            verify { repository.upsertSnapshot("sample:1", "Engineer", "ACME", "https://example.com/1", null) }
        }

        "skips a redelivered event (idempotent on event_id)" {
            val repository = mockk<TrackerRepository>()
            every { repository.markEventProcessed("evt-1") } returns false

            EnrichmentService(repository).apply(event())

            verify(exactly = 0) { repository.upsertSnapshot(any(), any(), any(), any(), any()) }
        }
    })
