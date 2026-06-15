package com.jobsearch.gateway.ratelimit

import org.springframework.web.server.ServerWebExchange

/**
 * Derives the rate-limit bucket key for a request. Prefers the left-most `X-Forwarded-For` hop (the
 * original client when behind a load balancer), then the socket remote address, then a shared
 * `"unknown"` bucket as a safe fallback so a missing address can never bypass the limit.
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
