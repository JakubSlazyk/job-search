package com.jobsearch.gateway.ratelimit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import java.net.InetSocketAddress

class ClientKeyResolverTest :
    StringSpec({
        val resolver = ClientKeyResolver()

        "prefers the left-most X-Forwarded-For hop" {
            val request =
                MockServerHttpRequest
                    .get("/api/v1/offers")
                    .header("X-Forwarded-For", "203.0.113.7, 70.41.3.18")
                    .remoteAddress(InetSocketAddress("10.0.0.1", 5000))
                    .build()

            resolver.resolve(MockServerWebExchange.from(request)) shouldBe "203.0.113.7"
        }

        "falls back to the socket remote address when no forwarded header" {
            val request =
                MockServerHttpRequest
                    .get("/api/v1/offers")
                    .remoteAddress(InetSocketAddress("10.0.0.1", 5000))
                    .build()

            resolver.resolve(MockServerWebExchange.from(request)) shouldBe "10.0.0.1"
        }

        "falls back to a shared 'unknown' bucket when no address is available" {
            val request = MockServerHttpRequest.get("/api/v1/offers").build()

            resolver.resolve(MockServerWebExchange.from(request)) shouldBe "unknown"
        }

        "ignores a blank forwarded header and uses the remote address" {
            val request =
                MockServerHttpRequest
                    .get("/api/v1/offers")
                    .header("X-Forwarded-For", "   ")
                    .remoteAddress(InetSocketAddress("198.51.100.2", 443))
                    .build()

            resolver.resolve(MockServerWebExchange.from(request)) shouldBe "198.51.100.2"
        }
    })
