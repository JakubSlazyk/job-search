package com.jobsearch.processing

import com.google.protobuf.Timestamp
import com.jobsearch.common.domain.OfferIdentity
import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.NormalizedOffer
import java.time.Instant

/**
 * Maps a [RawOffer] to a [NormalizedOffer], stamping the canonical `offer_id`. The walking skeleton
 * (§1.1) only assigns the identity and trims whitespace; real source-agnostic canonicalization
 * (salary/seniority/location) and cross-source dedupe land in §1.3.
 */
object OfferNormalizer {
    fun normalize(
        raw: RawOffer,
        normalizedAt: Instant = Instant.now(),
    ): NormalizedOffer =
        NormalizedOffer
            .newBuilder()
            .setOfferId(OfferIdentity.offerId(raw.source, raw.externalId))
            .setSource(raw.source)
            .setExternalId(raw.externalId)
            .setTitle(raw.title.trim())
            .setCompany(raw.company.trim())
            .setUrl(raw.url)
            .setLocation(raw.location.trim())
            .setDescription(raw.description.trim())
            .setNormalizedAt(
                Timestamp
                    .newBuilder()
                    .setSeconds(normalizedAt.epochSecond)
                    .setNanos(normalizedAt.nano)
                    .build(),
            ).build()
}
