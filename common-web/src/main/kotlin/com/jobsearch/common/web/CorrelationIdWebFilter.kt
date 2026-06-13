package com.jobsearch.common.web

import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Reactive-stack correlation filter: reuses an inbound [Correlation.HEADER] or mints a new id,
 * echoes it on the response, and writes it into the Reactor context (the reactive equivalent of the
 * MDC) so downstream operators and logging can read it.
 */
class CorrelationIdWebFilter : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val correlationId =
            exchange.request.headers
                .getFirst(Correlation.HEADER)
                ?.takeIf(String::isNotBlank)
                ?: UUID.randomUUID().toString()
        exchange.response.headers.set(Correlation.HEADER, correlationId)
        return chain
            .filter(exchange)
            .contextWrite { context -> context.put(Correlation.KEY, correlationId) }
    }
}
