package com.jobsearch.offer

import com.google.protobuf.Timestamp
import com.jobsearch.proto.offer.v1.OfferPublished
import java.time.Instant
import java.util.UUID

/**
 * Builds the [OfferPublished] event for an [Offer] persisted to the canonical store. The `event_id` is
 * derived deterministically from the offer's content, so a Kafka redelivery of the same normalized
 * offer (e.g. a post-commit indexing failure, a rebalance, or a restart) produces the *same* event id
 * and at-least-once consumers dedupe it away; a genuine content change yields a new id (ADR 0002).
 */
object OfferPublishedFactory {
    fun from(
        offer: Offer,
        publishedAt: Instant = Instant.now(),
        eventId: String = deterministicEventId(offer),
    ): OfferPublished =
        OfferPublished
            .newBuilder()
            .setEventId(eventId)
            .setOfferId(offer.offerId)
            .setPublishedAt(
                Timestamp
                    .newBuilder()
                    .setSeconds(publishedAt.epochSecond)
                    .setNanos(publishedAt.nano)
                    .build(),
            ).setTitle(offer.title)
            .setCompany(offer.company)
            .setUrl(offer.url)
            .build()

    /**
     * Stable id over the full offer content (every field except the time-based `publishedAt`). Hashing
     * all fields — not just the published title/company/url — means a change to a field a consumer
     * fetches separately (e.g. description, seniority) still emits a fresh event.
     */
    private fun deterministicEventId(offer: Offer): String =
        UUID
            .nameUUIDFromBytes(
                listOf(
                    offer.offerId,
                    offer.source,
                    offer.externalId,
                    offer.title,
                    offer.company,
                    offer.url,
                    offer.location,
                    offer.description,
                    offer.seniority,
                ).joinToString(separator = " ").toByteArray(Charsets.UTF_8),
            ).toString()
}
