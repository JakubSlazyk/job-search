package com.jobsearch.common.domain

/**
 * Canonical Kafka topic names for the offer pipeline, shared so producers and consumers agree.
 * See docs/phase-1-plan.md §1.0 and docs/architecture.md.
 */
object Topics {
    /** collection-service → processing-service. */
    const val RAW_OFFERS = "raw-offers"

    /** processing-service → offer-service. */
    const val NORMALIZED_OFFERS = "normalized-offers"

    /** offer-service → notification/tracker/chatbot (Phase 2/3). */
    const val OFFER_PUBLISHED = "offer.published"
}
