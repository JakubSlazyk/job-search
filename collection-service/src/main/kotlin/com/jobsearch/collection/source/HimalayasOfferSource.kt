package com.jobsearch.collection.source

import com.google.protobuf.ByteString
import com.jobsearch.collection.CollectionSourceProperties
import com.jobsearch.proto.collection.v1.RawOffer
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Open-API source for the Himalayas job board (`https://himalayas.app/jobs/api`). Unlike the generic
 * [HttpJsonOfferSource], the response is an *object* (`{ "jobs": [...] }`) with Himalayas-specific
 * field names, so it gets its own anti-corruption adapter (ADR 0005): it fetches a bounded page and
 * maps each `jobs[]` element to a uniform [RawOffer], retaining the original JSON as `raw_content`.
 * The outbound call is guarded by Resilience4j retry + circuit-breaker. No-ops when unconfigured.
 */
@Component
class HimalayasOfferSource(
    private val webClient: WebClient,
    private val properties: CollectionSourceProperties,
    retryRegistry: RetryRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val objectMapper: ObjectMapper,
) : OfferSource {
    override val sourceName = "himalayas"
    private val retry = retryRegistry.retry(sourceName)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(sourceName)

    override fun fetch(): Flux<RawOffer> {
        val url = properties.himalayasUrl
        if (url.isBlank()) return Flux.empty()
        val pagedUrl =
            UriComponentsBuilder
                .fromUriString(url)
                .replaceQueryParam("limit", properties.himalayasLimit)
                .build(true)
                .toUriString()
        return webClient
            .get()
            .uri(pagedUrl)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .flatMapMany { body -> Flux.fromIterable(parse(body)) }
    }

    private fun parse(body: String): List<RawOffer> =
        objectMapper
            .readTree(body)
            .path("jobs")
            .valueStream()
            .filter { it.path("guid").asString().isNotBlank() }
            .map(::toRawOffer)
            .toList()

    private fun toRawOffer(node: JsonNode): RawOffer =
        RawOffer
            .newBuilder()
            .setSource(sourceName)
            .setExternalId(externalIdOf(node.path("guid").asString()))
            .setFetchedAt(timestampOf())
            .setTitle(node.path("title").asString())
            .setCompany(node.path("companyName").asString())
            .setUrl(node.path("applicationLink").asString())
            .setLocation(locationOf(node.path("locationRestrictions")))
            .setDescription(node.path("description").asString())
            .setContentType("application/json")
            .setRawContent(ByteString.copyFromUtf8(node.toString()))
            .build()

    /**
     * A clean, path-safe, stable id. The Himalayas `guid` is a full URL
     * (`.../jobs/azure-cloud-architect-8965525546`); using it verbatim would put `:` and `/` into the
     * `offerId` and break REST detail routing. We take the trailing numeric job id (stable across
     * title/slug edits), falling back to the URL's last path segment when there is none.
     */
    private fun externalIdOf(guid: String): String {
        val slug = guid.substringAfterLast('/')
        return TRAILING_ID.find(slug)?.value ?: slug
    }

    /** Joins the country restrictions into a single label, defaulting to "Worldwide" when empty. */
    private fun locationOf(restrictions: JsonNode): String =
        restrictions
            .valueStream()
            .map { it.asString() }
            .filter(String::isNotBlank)
            .toList()
            .joinToString(", ")
            .ifBlank { "Worldwide" }

    private companion object {
        const val USER_AGENT = "job-search-collection-service (+https://github.com/jakubslazyk/job-search)"

        /** Trailing run of digits in a job slug — the stable Himalayas job id. */
        val TRAILING_ID = Regex("""\d+$""")
    }
}
