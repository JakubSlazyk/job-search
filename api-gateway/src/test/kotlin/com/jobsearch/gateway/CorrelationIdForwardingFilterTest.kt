package com.jobsearch.gateway

import com.jobsearch.common.web.Correlation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class CorrelationIdForwardingFilterTest :
    StringSpec({
        val filter = CorrelationIdForwardingFilter()

        "copies the correlation id from the response onto the proxied request" {
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/offers"))
            exchange.response.headers.set(Correlation.HEADER, "trace-abc")

            var forwarded: ServerWebExchange? = null
            val chain =
                GatewayFilterChain {
                    forwarded = it
                    Mono.empty()
                }

            filter.filter(exchange, chain).block()

            forwarded!!.request.headers.getFirst(Correlation.HEADER) shouldBe "trace-abc"
        }

        "passes the request through unchanged when no correlation id is present" {
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/offers"))

            var forwarded: ServerWebExchange? = null
            val chain =
                GatewayFilterChain {
                    forwarded = it
                    Mono.empty()
                }

            filter.filter(exchange, chain).block()

            forwarded!!
                .request.headers
                .getFirst(Correlation.HEADER)
                .shouldBeNull()
        }
    })
