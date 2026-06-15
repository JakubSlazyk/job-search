package com.jobsearch.gateway.ratelimit

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

/**
 * Per-client rate limiting for every proxied request (Phase 1.7). Resolves a client key (see
 * [ClientKeyResolver]), draws a permit from that key's [io.github.resilience4j.ratelimiter.RateLimiter]
 * in the [registry], and — when the per-period budget is exhausted — short-circuits with an RFC 9457
 * `429 Too Many Requests` instead of forwarding downstream.
 *
 * The limiter is configured with a zero acquire timeout, so `acquirePermission()` never blocks the
 * Netty event loop. Runs at highest precedence to reject before any routing work happens.
 */
class RateLimitingGlobalFilter(
    private val registry: RateLimiterRegistry,
    private val keyResolver: ClientKeyResolver,
) : GlobalFilter,
    Ordered {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val key = keyResolver.resolve(exchange)
        val permitted = registry.rateLimiter(key).acquirePermission()
        return if (permitted) chain.filter(exchange) else tooManyRequests(exchange)
    }

    private fun tooManyRequests(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
        val body =
            """
            {"type":"about:blank","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded; retry later."}
            """.trimIndent()
        val buffer = response.bufferFactory().wrap(body.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
