package com.jobsearch.common.web

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class CorrelationIdWebFilterTest :
    StringSpec({
        val filter = CorrelationIdWebFilter()

        "reuses an inbound correlation id, writes it to the Reactor context, and echoes it" {
            val exchange =
                MockServerWebExchange.from(
                    MockServerHttpRequest.get("/").header(Correlation.HEADER, "ctx-1"),
                )
            var seenInContext: String? = null
            val chain =
                WebFilterChain {
                    Mono.deferContextual { context ->
                        seenInContext = context.get(Correlation.KEY)
                        Mono.empty()
                    }
                }

            filter.filter(exchange, chain).block()

            seenInContext shouldBe "ctx-1"
            exchange.response.headers.getFirst(Correlation.HEADER) shouldBe "ctx-1"
        }

        "generates a correlation id when none is supplied" {
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
            val chain = WebFilterChain { Mono.empty() }

            filter.filter(exchange, chain).block()

            exchange.response.headers
                .getFirst(Correlation.HEADER)!!
                .shouldNotBeEmpty()
        }
    })
