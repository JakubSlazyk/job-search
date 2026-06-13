package com.jobsearch.processing.domain

import java.util.Collections

/**
 * Best-effort in-memory dedupe of offer ids over a bounded recent window (LRU). Processing is
 * stateless across restarts and instances, so cross-instance idempotency ultimately rests on
 * offer-service's upsert by `offer_id`; this just drops obvious in-window repeats so they don't
 * churn downstream.
 */
class OfferDeduplicator(
    capacity: Int = DEFAULT_CAPACITY,
) {
    private val seen: MutableSet<String> =
        Collections.newSetFromMap(
            object : LinkedHashMap<String, Boolean>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>): Boolean = size > capacity
            },
        )

    /** Records [offerId] and returns true if it was not already in the recent window. */
    @Synchronized
    fun firstSeen(offerId: String): Boolean = seen.add(offerId)

    private companion object {
        const val DEFAULT_CAPACITY = 10_000
        const val INITIAL_CAPACITY = 16
        const val LOAD_FACTOR = 0.75f
    }
}
