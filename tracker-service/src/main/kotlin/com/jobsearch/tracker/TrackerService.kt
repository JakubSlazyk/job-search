package com.jobsearch.tracker

import org.springframework.stereotype.Service

/** A tracked offer was requested for a user who is not tracking it. Mapped to a 404 by the advice. */
class TrackedOfferNotFoundException(
    offerId: String,
) : RuntimeException("No tracked offer '$offerId' for the current user")

/**
 * Application-tracker use cases, all self-scoped by the caller's `subject` (the controller takes it
 * from the relayed JWT — never from the client). [track] is an upsert (track-or-restate); [update]
 * and [untrack] require an existing row and 404 otherwise.
 */
@Service
class TrackerService(
    private val repository: TrackerRepository,
) {
    fun list(subject: String): List<TrackedOfferView> = repository.listApplications(subject)

    fun get(
        subject: String,
        offerId: String,
    ): TrackedOfferView = repository.findApplication(subject, offerId) ?: throw TrackedOfferNotFoundException(offerId)

    fun track(
        subject: String,
        request: TrackRequest,
    ): TrackedOfferView {
        repository.upsertApplication(subject, request.offerId, request.status, request.notes)
        return get(subject, request.offerId)
    }

    fun update(
        subject: String,
        offerId: String,
        request: UpdateRequest,
    ): TrackedOfferView {
        if (!repository.updateApplication(subject, offerId, request.status, request.notes)) {
            throw TrackedOfferNotFoundException(offerId)
        }
        return get(subject, offerId)
    }

    fun untrack(
        subject: String,
        offerId: String,
    ) {
        if (!repository.deleteApplication(subject, offerId)) throw TrackedOfferNotFoundException(offerId)
    }
}
