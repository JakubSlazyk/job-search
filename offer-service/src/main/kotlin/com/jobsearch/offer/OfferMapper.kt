package com.jobsearch.offer

import com.jobsearch.proto.processing.v1.NormalizedOffer

/** Maps a consumed [NormalizedOffer] to the persisted [Offer]. */
object OfferMapper {
    fun toOffer(normalized: NormalizedOffer): Offer =
        Offer(
            offerId = normalized.offerId,
            source = normalized.source,
            externalId = normalized.externalId,
            title = normalized.title,
            company = normalized.company,
            url = normalized.url,
            location = normalized.location,
            description = normalized.description,
            seniority = normalized.seniority.name,
        )
}
