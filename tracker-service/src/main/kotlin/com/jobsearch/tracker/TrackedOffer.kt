package com.jobsearch.tracker

import java.time.Instant

/** Where an offer sits in the user's application pipeline. Validated server-side; unknown values 400. */
enum class ApplicationStatus {
    SAVED,
    APPLIED,
    REJECTED,
}

/** A tracked offer for one user — the (subject, offerId) the user is following, with their notes. */
data class TrackedOffer(
    val subject: String,
    val offerId: String,
    val status: ApplicationStatus,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Local denormalized copy of the offer fields the tracker renders, fed by `offer.published`
 * (enrichment). Absent until the matching event arrives — tracking an offer never waits on it.
 */
data class OfferSnapshot(
    val offerId: String,
    val title: String?,
    val company: String?,
    val url: String?,
    val publishedAt: Instant?,
)

/** A tracked offer together with its (possibly absent) display snapshot. */
data class TrackedOfferView(
    val tracked: TrackedOffer,
    val snapshot: OfferSnapshot?,
)
