package com.jobsearch.notification

import com.jobsearch.notification.delivery.EmailSender
import com.jobsearch.notification.delivery.NotificationStream
import com.jobsearch.notification.grpc.UserContactClient
import com.jobsearch.proto.offer.v1.OfferPublished
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Core matcher. For each `offer.published` event (deduped on `event_id`), it finds the subjects whose
 * criteria match and delivers to each: records the delivery (history), pushes it on the live WebSocket
 * stream, and — when the user has a contact email and opted into email — sends an email. Contact
 * resolution and email are best-effort; a missing user or opt-out simply skips the email, never the
 * recorded/WebSocket delivery.
 */
@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val userContactClient: UserContactClient,
    private val emailSender: EmailSender,
    private val stream: NotificationStream,
) {
    fun onOfferPublished(event: OfferPublished): Mono<Void> =
        repository
            .markEventProcessed(event.eventId)
            .filter { firstTime -> firstTime }
            .flatMapMany { repository.findMatchingSubjects(event.title, event.company) }
            .flatMap { subject -> deliver(subject, event.toMatch()) }
            .then()

    private fun deliver(
        subject: String,
        match: OfferMatch,
    ): Mono<Void> =
        repository
            .recordDelivery(subject, match)
            .flatMap { delivered ->
                stream.publish(delivered)
                userContactClient
                    .resolve(subject)
                    .filter { it.emailNotificationsEnabled && it.email.isNotBlank() }
                    .flatMap { contact -> emailSender.send(contact.email, contact.displayName, match) }
            }.then()

    private fun OfferPublished.toMatch(): OfferMatch = OfferMatch(offerId, title, company, url)
}
