package com.jobsearch.gateway.ratelimit

import org.springframework.web.server.ServerWebExchange

/**
 * Derives the rate-limit bucket key for a request. Prefers the left-most `X-Forwarded-For` hop (the
 * original client when behind a load balancer), then the socket remote address, then a shared
 * `"unknown"` bucket as a safe fallback so a missing address can never bypass the limit.
 *
 * Trust assumption: the left-most `X-Forwarded-For` hop is client-controllable, so it is only
 * trustworthy when the gateway sits behind a trusted proxy chain (e.g. an AWS ALB that overwrites the
 * header). If the gateway is ever directly reachable, a client could forge/rotate this header to
 * dodge its per-client budget. Trusted-proxy-aware resolution (Spring's
 * `XForwardedRemoteAddressResolver` with a configurable trusted-hop index) is deferred to Phase 2
 * alongside the BFF (ADR 0004); the bounded limiter cache (see [PerClientRateLimiters]) caps the
 * memory cost of spoofed keys in the meantime.
 */
class ClientKeyResolver {
    fun resolve(exchange: ServerWebExchange): String {
        val forwarded =
            exchange.request.headers
                .getFirst("X-Forwarded-For")
                ?.substringBefore(',')
                ?.trim()
                ?.takeIf(String::isNotBlank)
        if (forwarded != null) return forwarded
        return exchange.request.remoteAddress
            ?.address
            ?.hostAddress
            ?: "unknown"
    }
}
