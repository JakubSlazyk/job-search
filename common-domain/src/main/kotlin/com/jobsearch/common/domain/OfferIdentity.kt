package com.jobsearch.common.domain

/**
 * Builds the stable identity of an offer — `"<source>:<externalId>"`.
 *
 * This identity is the Kafka message key, the dedupe key in processing-service, and the
 * upsert/outbox key in offer-service, so it is centralized here for every service to agree on.
 * See ADR 0005 and docs/phase-1-plan.md §1.0.
 */
object OfferIdentity {
    private const val SEPARATOR = ":"

    /**
     * @throws IllegalArgumentException if [source] or [externalId] is blank.
     */
    fun offerId(
        source: String,
        externalId: String,
    ): String {
        require(source.isNotBlank()) { "source must not be blank" }
        require(externalId.isNotBlank()) { "externalId must not be blank" }
        return "$source$SEPARATOR$externalId"
    }
}
