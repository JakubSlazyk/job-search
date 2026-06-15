package com.jobsearch.gateway

import com.jobsearch.gateway.ratelimit.ClientKeyResolver
import com.jobsearch.gateway.ratelimit.PerClientRateLimiters
import com.jobsearch.gateway.ratelimit.RateLimitProperties
import com.jobsearch.gateway.ratelimit.RateLimitingGlobalFilter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import kotlin.math.ceil

/**
 * Wires the gateway's cross-cutting concerns (Phase 1.7): correlation-id propagation and the
 * in-process rate limiter. Routing itself is declarative in `application.yml`.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties::class)
class GatewayConfiguration {
    @Bean
    fun correlationIdForwardingFilter(): CorrelationIdForwardingFilter = CorrelationIdForwardingFilter()

    @Bean
    fun clientKeyResolver(): ClientKeyResolver = ClientKeyResolver()

    /** Bounded cache of one [io.github.resilience4j.ratelimiter.RateLimiter] per client key. */
    @Bean
    fun perClientRateLimiters(properties: RateLimitProperties): PerClientRateLimiters {
        val config =
            RateLimiterConfig
                .custom()
                .limitForPeriod(properties.limitForPeriod)
                .limitRefreshPeriod(properties.refreshPeriod)
                .timeoutDuration(Duration.ZERO) // non-blocking: never parks the Netty event loop
                .build()
        return PerClientRateLimiters(config, properties.maxClients, properties.idleTtl)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "gateway.rate-limit",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun rateLimitingGlobalFilter(
        limiters: PerClientRateLimiters,
        keyResolver: ClientKeyResolver,
        properties: RateLimitProperties,
    ): RateLimitingGlobalFilter =
        RateLimitingGlobalFilter(limiters, keyResolver, retryAfterSeconds(properties.refreshPeriod))

    /** Refresh period as whole seconds (rounded up, min 1) for the `429` `Retry-After` hint. */
    private fun retryAfterSeconds(refreshPeriod: Duration): Long =
        maxOf(1L, ceil(refreshPeriod.toMillis() / 1000.0).toLong())
}
