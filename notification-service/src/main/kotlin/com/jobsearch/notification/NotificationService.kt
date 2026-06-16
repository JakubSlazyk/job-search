package com.jobsearch.notification

import com.jobsearch.notification.delivery.EmailSender
import com.jobsearch.notification.delivery.NotificationStream
import com.jobsearch.notification.grpc.UserContactClient
import com.jobsearch.proto.offer.v1.OfferPublished
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Core matcher. For each `offer.published` event (deduped on `event_id`), it finds the subjects whose
 * criteria match and records a delivery row for each.
 *
 * The dedup guard and the delivery rows are written in **one transaction** ([persistMatches]) so the
 * event is recorded as processed if and only if its deliveries are persisted — a transient failure
 * rolls back both and the consumer retries (idempotency holds; no half-applied event). This is the
 * same all-or-nothing guarantee tracker-service gets from its `@Transactional` enrichment.
 *
 * The live WebSocket push and the email run **after** the commit ([dispatch]) as best-effort side
 * effects: they never roll back a recorded delivery, and a missing user or email opt-out simply skips
 * the email. History (the delivery rows) is the source of truth; the WebSocket feed is at-most-once.
 */
@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val userContactClient: UserContactClient,
    private val emailSender: EmailSender,
    private val stream: NotificationStream,
    private val transactionalOperator: TransactionalOperator,
) {
    fun onOfferPublished(event: OfferPublished): Mono<Void> {
        val match = event.toMatch()
        return persistMatches(event.eventId, match)
            .flatMap { delivered -> dispatch(delivered, match) }
            .then()
    }

    /** Dedup guard + one delivery row per matching subject, atomically. Empty when already processed. */
    private fun persistMatches(
        eventId: String,
        match: OfferMatch,
    ): Flux<DeliveredNotification> {
        val matches =
            repository
                .markEventProcessed(eventId)
                .filter { firstTime -> firstTime }
                .flatMapMany { repository.findMatchingSubjects(match.title, match.company) }
                .flatMap { subject -> repository.recordDelivery(subject, match) }
        return transactionalOperator.transactional(matches)
    }

    /** Post-commit, best-effort: push the match on the live stream and email opted-in users. */
    private fun dispatch(
        delivered: DeliveredNotification,
        match: OfferMatch,
    ): Mono<Void> {
        stream.publish(delivered)
        return userContactClient
            .resolve(delivered.subject)
            .filter { it.emailNotificationsEnabled && it.email.isNotBlank() }
            .flatMap { contact -> emailSender.send(contact.email, contact.displayName, match) }
            .then()
    }

    private fun OfferPublished.toMatch(): OfferMatch = OfferMatch(offerId, title, company, url)
}
