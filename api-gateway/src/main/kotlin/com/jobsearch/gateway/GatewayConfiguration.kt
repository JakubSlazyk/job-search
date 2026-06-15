package com.jobsearch.gateway

import com.jobsearch.gateway.ratelimit.ClientKeyResolver
import com.jobsearch.gateway.ratelimit.RateLimitProperties
import com.jobsearch.gateway.ratelimit.RateLimitingGlobalFilter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

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

    /** One registry; a [io.github.resilience4j.ratelimiter.RateLimiter] is created lazily per client key. */
    @Bean
    fun rateLimiterRegistry(properties: RateLimitProperties): RateLimiterRegistry {
        val config =
            RateLimiterConfig
                .custom()
                .limitForPeriod(properties.limitForPeriod)
                .limitRefreshPeriod(properties.refreshPeriod)
                .timeoutDuration(Duration.ZERO) // non-blocking: never parks the Netty event loop
                .build()
        return RateLimiterRegistry.of(config)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "gateway.rate-limit",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun rateLimitingGlobalFilter(
        registry: RateLimiterRegistry,
        keyResolver: ClientKeyResolver,
    ): RateLimitingGlobalFilter = RateLimitingGlobalFilter(registry, keyResolver)
}
