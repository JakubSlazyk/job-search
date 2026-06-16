package com.jobsearch.notification

import com.jobsearch.notification.delivery.EmailSender
import com.jobsearch.notification.delivery.NotificationStream
import com.jobsearch.notification.grpc.UserContactClient
import com.jobsearch.proto.offer.v1.OfferPublished
import com.jobsearch.proto.user.v1.UserContact
import io.kotest.core.spec.style.StringSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

class NotificationServiceTest :
    StringSpec({
        val repository = mockk<NotificationRepository>()
        val userContactClient = mockk<UserContactClient>()
        val emailSender = mockk<EmailSender>(relaxed = true)
        val stream = mockk<NotificationStream>(relaxed = true)
        // Pass-through transactional operator: the atomicity it provides is exercised by the
        // Testcontainers IT; here it must just run the wrapped Flux unchanged.
        val transactionalOperator =
            mockk<TransactionalOperator> {
                every { transactional(any<Flux<DeliveredNotification>>()) } answers { firstArg() }
            }
        val service =
            NotificationService(repository, userContactClient, emailSender, stream, transactionalOperator)

        beforeTest { clearMocks(repository, userContactClient, emailSender, stream, answers = false) }

        val event =
            OfferPublished
                .newBuilder()
                .setEventId("evt-1")
                .setOfferId("sample:1")
                .setTitle("Senior Kotlin Engineer")
                .setCompany("ACME")
                .setUrl("https://example.com/1")
                .build()

        fun contact(emailEnabled: Boolean) =
            UserContact
                .newBuilder()
                .setSubject("s1")
                .setEmail("s1@example.com")
                .setDisplayName("Sam")
                .setEmailNotificationsEnabled(emailEnabled)
                .build()

        fun delivered() =
            DeliveredNotification(
                1,
                "s1",
                "sample:1",
                "Senior Kotlin Engineer",
                "ACME",
                "https://example.com/1",
                Instant.now(),
            )

        "skips an already-processed event (idempotent on event_id)" {
            every { repository.markEventProcessed("evt-1") } returns Mono.just(false)

            StepVerifier.create(service.onOfferPublished(event)).verifyComplete()

            verify(exactly = 0) { repository.findMatchingSubjects(any(), any()) }
        }

        "delivers to a matching subject and emails when opted in" {
            every { repository.markEventProcessed("evt-1") } returns Mono.just(true)
            every { repository.findMatchingSubjects("Senior Kotlin Engineer", "ACME") } returns Flux.just("s1")
            every { repository.recordDelivery("s1", any()) } returns Mono.just(delivered())
            every { userContactClient.resolve("s1") } returns Mono.just(contact(emailEnabled = true))
            every { emailSender.send(any(), any(), any()) } returns Mono.empty()

            StepVerifier.create(service.onOfferPublished(event)).verifyComplete()

            verify { stream.publish(any()) }
            verify { emailSender.send("s1@example.com", "Sam", any()) }
        }

        "delivers but skips email when the user opted out" {
            every { repository.markEventProcessed("evt-1") } returns Mono.just(true)
            every { repository.findMatchingSubjects(any(), any()) } returns Flux.just("s1")
            every { repository.recordDelivery("s1", any()) } returns Mono.just(delivered())
            every { userContactClient.resolve("s1") } returns Mono.just(contact(emailEnabled = false))

            StepVerifier.create(service.onOfferPublished(event)).verifyComplete()

            verify { stream.publish(any()) }
            verify(exactly = 0) { emailSender.send(any(), any(), any()) }
        }

        "delivers but skips email when the user is unknown" {
            every { repository.markEventProcessed("evt-1") } returns Mono.just(true)
            every { repository.findMatchingSubjects(any(), any()) } returns Flux.just("s1")
            every { repository.recordDelivery("s1", any()) } returns Mono.just(delivered())
            every { userContactClient.resolve("s1") } returns Mono.empty()

            StepVerifier.create(service.onOfferPublished(event)).verifyComplete()

            verify { stream.publish(any()) }
            verify(exactly = 0) { emailSender.send(any(), any(), any()) }
        }
    })
