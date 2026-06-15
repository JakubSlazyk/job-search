package com.jobsearch.gateway.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import java.time.Duration

/**
 * Holds one Resilience4j [RateLimiter] per client key, behind a bounded Caffeine cache (Phase 1.7).
 *
 * Resilience4j's own `RateLimiterRegistry` never evicts, so keying it by client (a high-cardinality,
 * partly client-controlled value — see [ClientKeyResolver]) would let the map grow without bound.
 * This cache caps retained keys at [maxClients] and evicts any key idle for [idleTtl]; an evicted
 * client just starts a fresh budget on its next request, which is harmless for rate limiting.
 *
 * All limiters share one [config], so each key gets the same per-period budget.
 */
class PerClientRateLimiters(
    private val config: RateLimiterConfig,
    maxClients: Long,
    idleTtl: Duration,
) {
    private val cache: Cache<String, RateLimiter> =
        Caffeine
            .newBuilder()
            .maximumSize(maxClients)
            .expireAfterAccess(idleTtl)
            .build()

    /** Draws a permit from [key]'s limiter (creating it on first use); `false` when its budget is spent. */
    fun acquirePermission(key: String): Boolean = cache.get(key) { RateLimiter.of(it, config) }.acquirePermission()
}
