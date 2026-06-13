package com.jobsearch.collection

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/** Wires the WebClient and the shared Resilience4j retry / circuit-breaker used by source fetches. */
@Configuration
@EnableConfigurationProperties(CollectionSourceProperties::class)
class CollectionConfig {
    @Bean
    fun webClient(): WebClient = WebClient.create()

    @Bean
    fun fetchRetry(): Retry =
        Retry.of(
            "source-fetch",
            RetryConfig
                .custom<Any>()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .build(),
        )

    @Bean
    fun fetchCircuitBreaker(): CircuitBreaker =
        CircuitBreaker.of(
            "source-fetch",
            CircuitBreakerConfig
                .custom()
                .failureRateThreshold(50f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build(),
        )
}
