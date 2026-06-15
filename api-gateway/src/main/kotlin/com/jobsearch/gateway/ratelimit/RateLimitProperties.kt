package com.jobsearch.gateway.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Tuning for the in-process gateway rate limiter (Phase 1.7). Each distinct client key gets up to
 * [limitForPeriod] requests per [refreshPeriod]; over that, the gateway replies `429`.
 *
 * The per-client limiters live in a bounded cache: at most [maxClients] keys are retained and a key
 * idle for [idleTtl] is evicted. This caps memory so high-cardinality (or spoofed) client keys can't
 * grow the heap without limit — an evicted client simply starts a fresh budget on its next request.
 *
 * This is a single-instance limiter (Resilience4j, in-memory): correct for the local/MVP gateway but
 * not shared across replicas. Distributed Redis-backed limiting arrives with the BFF in Phase 2
 * (ADR 0004).
 */
@ConfigurationProperties(prefix = "gateway.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val limitForPeriod: Int = 100,
    val refreshPeriod: Duration = Duration.ofSeconds(1),
    val maxClients: Long = 100_000,
    val idleTtl: Duration = Duration.ofMinutes(10),
)
