package com.jobsearch.processing.application

import com.jobsearch.processing.domain.OfferCanonicalizer
import com.jobsearch.processing.domain.OfferDeduplicator
import com.jobsearch.proto.collection.v1.RawOffer

/**
 * Application service driving the transform: canonicalize a raw offer, drop in-window duplicates,
 * and emit the rest through the [NormalizedOfferSink] port. Framework-free by design.
 */
class OfferProcessor(
    private val canonicalizer: OfferCanonicalizer,
    private val deduplicator: OfferDeduplicator,
    private val sink: NormalizedOfferSink,
) {
    /** Returns true if the offer was emitted, false if it was deduplicated. */
    fun process(raw: RawOffer): Boolean {
        val normalized = canonicalizer.canonicalize(raw)
        if (!deduplicator.firstSeen(normalized.offerId)) return false
        sink.send(normalized)
        return true
    }
}
