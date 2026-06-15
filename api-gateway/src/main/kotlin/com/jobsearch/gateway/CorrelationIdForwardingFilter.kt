package com.jobsearch.gateway

import com.jobsearch.common.web.Correlation
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Propagates the correlation id onto the proxied request so it reaches downstream services (Phase
 * 1.7). common-web's [com.jobsearch.common.web.CorrelationIdWebFilter] runs first (every WebFilter
 * precedes the gateway's GlobalFilters): it reuses or mints the id and echoes it on the response.
 * This filter copies that id from the response header onto the outbound request headers — the gateway
 * forwards an inbound id automatically, but a freshly minted one would otherwise never travel
 * downstream.
 *
 * Ordered ahead of the routing filters (which run at very low precedence) so the mutated request is
 * the one actually sent.
 */
class CorrelationIdForwardingFilter :
    GlobalFilter,
    Ordered {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val correlationId = exchange.response.headers.getFirst(Correlation.HEADER)
        if (correlationId.isNullOrBlank()) return chain.filter(exchange)
        val mutated =
            exchange
                .mutate()
                .request { it.header(Correlation.HEADER, correlationId) }
                .build()
        return chain.filter(mutated)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 10
}
