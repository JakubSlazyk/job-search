package com.jobsearch.collection

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Wires the WebClient and the Resilience4j retry / circuit-breaker registries used by source fetches.
 * Each source draws its *own* named retry/breaker from these registries (keyed by `sourceName`), so one
 * failing source can't trip the breaker for another — the per-source isolation ADR 0005 calls for.
 */
@Configuration
@EnableConfigurationProperties(CollectionSourceProperties::class)
class CollectionConfig {
    /** Response timeout so a hung upstream fails (and feeds the breaker) instead of stalling forever. */
    @Bean
    fun webClient(): WebClient =
        WebClient
            .builder()
            // A full job page (e.g. Himalayas with 50 rich HTML descriptions) easily exceeds the 256 KB
            // default in-memory buffer; without this the body decode throws and the source silently
            // collects nothing. Raise the cap to a generous bound for a single bounded page.
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
            .clientConnector(
                ReactorClientHttpConnector(HttpClient.create().responseTimeout(RESPONSE_TIMEOUT)),
            ).build()

    /** Shared retry config; `retryRegistry.retry(sourceName)` hands each source its own instance. */
    @Bean
    fun retryRegistry(): RetryRegistry =
        RetryRegistry.of(
            RetryConfig
                .custom<Any>()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .build(),
        )

    /** Shared breaker config; `circuitBreakerRegistry.circuitBreaker(sourceName)` isolates each source. */
    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            CircuitBreakerConfig
                .custom()
                .failureRateThreshold(50f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build(),
        )

    private companion object {
        val RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(10)

        /** In-memory buffer cap for a single fetched page; well above any one bounded source response. */
        const val MAX_IN_MEMORY_SIZE = 8 * 1024 * 1024
    }
}
