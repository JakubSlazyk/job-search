package com.jobsearch.collection.source

import com.jobsearch.proto.collection.v1.RawOffer
import reactor.core.publisher.Flux

/**
 * A single upstream offer source — an open API or a scraped site. One adapter per source: the
 * anti-corruption layer that fetches and maps the source's own shape into a uniform [RawOffer]
 * (ADR 0005). Adding a source means adding one implementation; nothing downstream changes.
 */
interface OfferSource {
    /** Stable identifier of this source; becomes the `source` field / part of the offer_id. */
    val sourceName: String

    /** Fetches the current offers from this source as uniform [RawOffer]s. */
    fun fetch(): Flux<RawOffer>
}
