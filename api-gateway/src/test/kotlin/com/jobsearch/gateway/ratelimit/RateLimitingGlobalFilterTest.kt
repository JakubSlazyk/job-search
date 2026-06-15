package com.jobsearch.gateway.ratelimit

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

class RateLimitingGlobalFilterTest :
    StringSpec({
        fun limitersAllowing(perPeriod: Int): PerClientRateLimiters =
            PerClientRateLimiters(
                RateLimiterConfig
                    .custom()
                    .limitForPeriod(perPeriod)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ZERO)
                    .build(),
                maxClients = 1_000,
                idleTtl = Duration.ofMinutes(10),
            )

        fun requestFrom(client: String): ServerWebExchange =
            MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/offers").header("X-Forwarded-For", client),
            )

        "forwards the request while permits remain" {
            val filter = RateLimitingGlobalFilter(limitersAllowing(1), ClientKeyResolver(), retryAfterSeconds = 1)
            val exchange = requestFrom("203.0.113.1")
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

        "short-circuits with an RFC 9457 429 (with Retry-After) once the budget is exhausted" {
            val filter = RateLimitingGlobalFilter(limitersAllowing(1), ClientKeyResolver(), retryAfterSeconds = 60)
            val chain = GatewayFilterChain { Mono.empty() }

            filter.filter(requestFrom("203.0.113.2"), chain).block()
            val second = requestFrom("203.0.113.2")
            filter.filter(second, chain).block()

            second.response.statusCode shouldBe HttpStatus.TOO_MANY_REQUESTS
            second.response.headers.contentType shouldBe MediaType.APPLICATION_PROBLEM_JSON
            second.response.headers.getFirst(HttpHeaders.RETRY_AFTER) shouldBe "60"
        }

        "keeps a separate budget per client key" {
            val filter = RateLimitingGlobalFilter(limitersAllowing(1), ClientKeyResolver(), retryAfterSeconds = 1)
            val noop = GatewayFilterChain { Mono.empty() }

            // client A spends its single permit, then is throttled
            filter.filter(requestFrom("198.51.100.1"), noop).block()
            val aSecond = requestFrom("198.51.100.1")
            filter.filter(aSecond, noop).block()

            // client B is unaffected by A's exhausted budget
            var bForwarded = false
            val bFirst = requestFrom("198.51.100.2")
            filter
                .filter(
                    bFirst,
                    GatewayFilterChain {
                        bForwarded = true
                        Mono.empty()
                    },
                ).block()

            aSecond.response.statusCode shouldBe HttpStatus.TOO_MANY_REQUESTS
            bForwarded shouldBe true
            bFirst.response.statusCode shouldBe null
        }
    })
