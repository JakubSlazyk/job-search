package com.jobsearch.gateway.ratelimit

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class PerClientRateLimitersTest :
    StringSpec({
        fun limiters(perPeriod: Int): PerClientRateLimiters =
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

        "spends a key's per-period budget then denies further permits (one bucket per key)" {
            val limiters = limiters(perPeriod = 2)

            limiters.acquirePermission("client-a") shouldBe true
            limiters.acquirePermission("client-a") shouldBe true
            limiters.acquirePermission("client-a") shouldBe false
        }

        "tracks each key's budget independently" {
            val limiters = limiters(perPeriod = 1)

            limiters.acquirePermission("client-a") shouldBe true
            limiters.acquirePermission("client-a") shouldBe false
            // a different key still has its full budget
            limiters.acquirePermission("client-b") shouldBe true
        }
    })
