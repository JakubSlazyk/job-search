package com.jobsearch.offer

import com.google.protobuf.Timestamp
import com.jobsearch.proto.offer.v1.OfferPublished
import java.time.Instant
import java.util.UUID

/**
 * Builds the [OfferPublished] event for an [Offer] persisted to the canonical store. Each event gets
 * a fresh `event_id`; consumers dedupe on it because CDC delivery is at-least-once (ADR 0002).
 */
object OfferPublishedFactory {
    fun from(
        offer: Offer,
        publishedAt: Instant = Instant.now(),
        eventId: String = UUID.randomUUID().toString(),
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
}
