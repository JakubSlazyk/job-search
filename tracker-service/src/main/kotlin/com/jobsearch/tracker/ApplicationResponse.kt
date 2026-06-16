package com.jobsearch.tracker

import java.time.Instant

/** Public shape of a tracked offer. [offer] is null until the enrichment event for it arrives. */
data class ApplicationResponse(
    val offerId: String,
    val status: ApplicationStatus,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val offer: OfferSummary?,
) {
    companion object {
        fun from(view: TrackedOfferView): ApplicationResponse =
            with(view.tracked) {
                ApplicationResponse(
                    offerId = offerId,
                    status = status,
                    notes = notes,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    offer = view.snapshot?.let { OfferSummary(it.title, it.company, it.url, it.publishedAt) },
                )
            }
    }
}

/** The denormalized offer fields the tracker renders, copied from `offer.published`. */
data class OfferSummary(
    val title: String?,
    val company: String?,
    val url: String?,
    val publishedAt: Instant?,
)
