package com.jobsearch.gateway.ratelimit

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

class RateLimitingGlobalFilterTest :
    StringSpec({
        fun registryAllowing(perPeriod: Int): RateLimiterRegistry =
            RateLimiterRegistry.of(
                RateLimiterConfig
                    .custom()
                    .limitForPeriod(perPeriod)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ZERO)
                    .build(),
            )

        "forwards the request while permits remain" {
            val filter = RateLimitingGlobalFilter(registryAllowing(1), ClientKeyResolver())
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/offers"))
            var forwarded = false
            val chain =
                GatewayFilterChain {
                    forwarded = true
                    Mono.empty()
                }

            filter.filter(exchange, chain).block()

            forwarded shouldBe true
            exchange.response.statusCode shouldBe null
        }

        "short-circuits with an RFC 9457 429 once the budget is exhausted" {
            val registry = registryAllowing(1)
            val filter = RateLimitingGlobalFilter(registry, ClientKeyResolver())
            val first = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/offers"))
            val second = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/offers"))
            val chain = GatewayFilterChain { Mono.empty() }

            filter.filter(first, chain).block()
            filter.filter(second, chain).block()

            second.response.statusCode shouldBe HttpStatus.TOO_MANY_REQUESTS
            second.response.headers.contentType shouldBe MediaType.APPLICATION_PROBLEM_JSON
        }
    })
