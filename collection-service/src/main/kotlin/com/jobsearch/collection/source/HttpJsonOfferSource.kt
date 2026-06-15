package com.jobsearch.collection.source

import com.google.protobuf.ByteString
import com.jobsearch.collection.CollectionSourceProperties
import com.jobsearch.proto.collection.v1.RawOffer
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Open-API source: fetches a JSON array of offers via [WebClient] and maps each element to a
 * uniform [RawOffer], retaining the original JSON element as `raw_content`. The outbound call is
 * guarded by Resilience4j retry + circuit-breaker. No-ops when its URL is unconfigured.
 */
@Component
class HttpJsonOfferSource(
    private val webClient: WebClient,
    private val properties: CollectionSourceProperties,
    retryRegistry: RetryRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val objectMapper: ObjectMapper,
) : OfferSource {
    override val sourceName = "http-json"
    private val retry = retryRegistry.retry(sourceName)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(sourceName)

    override fun fetch(): Flux<RawOffer> {
        val url = properties.httpJsonUrl
        if (url.isBlank()) return Flux.empty()
        return webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .flatMapMany { body -> Flux.fromIterable(parse(body)) }
    }

    private fun parse(body: String): List<RawOffer> =
        objectMapper
            .readTree(body)
            .valueStream()
            .map(::toRawOffer)
            .toList()

    private fun toRawOffer(node: JsonNode): RawOffer =
        RawOffer
            .newBuilder()
            .setSource(sourceName)
            .setExternalId(node.path("id").asString())
            .setFetchedAt(timestampOf())
            .setTitle(node.path("title").asString())
            .setCompany(node.path("company").asString())
            .setUrl(node.path("url").asString())
            .setLocation(node.path("location").asString())
            .setDescription(node.path("description").asString())
            .setContentType("application/json")
            .setRawContent(ByteString.copyFromUtf8(node.toString()))
            .build()
}
