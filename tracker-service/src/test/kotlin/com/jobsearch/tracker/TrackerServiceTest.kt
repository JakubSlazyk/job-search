package com.jobsearch.tracker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class TrackerServiceTest :
    StringSpec({
        val repository = mockk<TrackerRepository>()
        val service = TrackerService(repository)

        fun view(
            subject: String = "s1",
            offerId: String = "sample:1",
            status: ApplicationStatus = ApplicationStatus.SAVED,
            snapshot: OfferSnapshot? = null,
        ) = TrackedOfferView(
            TrackedOffer(subject, offerId, status, null, Instant.now(), Instant.now()),
            snapshot,
        )

        "track upserts then returns the enriched view" {
            val request = TrackRequest("sample:1", ApplicationStatus.APPLIED, "applied today")
            every {
                repository.upsertApplication("s1", "sample:1", ApplicationStatus.APPLIED, "applied today")
            } just Runs
            every { repository.findApplication("s1", "sample:1") } returns view(status = ApplicationStatus.APPLIED)

            val result = service.track("s1", request)

            result.tracked.offerId shouldBe "sample:1"
            verify { repository.upsertApplication("s1", "sample:1", ApplicationStatus.APPLIED, "applied today") }
        }

        "get returns the view when tracked" {
            every { repository.findApplication("s1", "sample:1") } returns view()

            service.get("s1", "sample:1").tracked.offerId shouldBe "sample:1"
        }

        "get throws when the offer is not tracked" {
            every { repository.findApplication("s1", "missing") } returns null

            shouldThrow<TrackedOfferNotFoundException> { service.get("s1", "missing") }
        }

        "update applies the change and returns the view" {
            val request = UpdateRequest(ApplicationStatus.REJECTED, "no fit")
            every { repository.updateApplication("s1", "sample:1", ApplicationStatus.REJECTED, "no fit") } returns true
            every { repository.findApplication("s1", "sample:1") } returns view(status = ApplicationStatus.REJECTED)

            service.update("s1", "sample:1", request).tracked.status shouldBe ApplicationStatus.REJECTED
        }

        "update throws when the offer is not tracked" {
            every { repository.updateApplication("s1", "missing", any(), any()) } returns false

            shouldThrow<TrackedOfferNotFoundException> {
                service.update("s1", "missing", UpdateRequest(ApplicationStatus.SAVED, null))
            }
        }

        "untrack throws when there was nothing to remove" {
            every { repository.deleteApplication("s1", "missing") } returns false

            shouldThrow<TrackedOfferNotFoundException> { service.untrack("s1", "missing") }
        }

        "untrack succeeds when a row is removed" {
            every { repository.deleteApplication("s1", "sample:1") } returns true

            service.untrack("s1", "sample:1")

            verify { repository.deleteApplication("s1", "sample:1") }
        }

        "list returns the user's tracked offers" {
            every { repository.listApplications("s1") } returns listOf(view(), view(offerId = "sample:2"))

            service.list("s1").map { it.tracked.offerId } shouldBe listOf("sample:1", "sample:2")
        }
    })
