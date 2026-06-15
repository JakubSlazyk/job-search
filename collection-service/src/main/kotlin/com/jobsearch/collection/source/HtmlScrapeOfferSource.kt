package com.jobsearch.collection.source

import com.google.protobuf.ByteString
import com.jobsearch.collection.CollectionSourceProperties
import com.jobsearch.proto.collection.v1.RawOffer
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

/**
 * Scrape source: fetches an HTML page via [WebClient] (non-blocking I/O) and extracts offers with
 * [Jsoup], retaining each offer's original HTML element as `raw_content`. The outbound call is
 * guarded by Resilience4j retry + circuit-breaker. No-ops when its URL is unconfigured.
 */
@Component
class HtmlScrapeOfferSource(
    private val webClient: WebClient,
    private val properties: CollectionSourceProperties,
    retryRegistry: RetryRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) : OfferSource {
    override val sourceName = "html-scrape"
    private val retry = retryRegistry.retry(sourceName)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(sourceName)

    override fun fetch(): Flux<RawOffer> {
        val url = properties.htmlScrapeUrl
        if (url.isBlank()) return Flux.empty()
        return webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .flatMapMany { html -> Flux.fromIterable(parse(html)) }
    }

    private fun parse(html: String): List<RawOffer> = Jsoup.parse(html).select("div.offer").map(::toRawOffer)

    private fun toRawOffer(element: Element): RawOffer =
        RawOffer
            .newBuilder()
            .setSource(sourceName)
            .setExternalId(element.attr("data-id"))
            .setFetchedAt(timestampOf())
            .setTitle(element.selectFirst("h2.title")?.text().orEmpty())
            .setCompany(element.selectFirst("span.company")?.text().orEmpty())
            .setUrl(element.selectFirst("a.link")?.attr("href").orEmpty())
            .setLocation(element.selectFirst("span.location")?.text().orEmpty())
            .setDescription(element.selectFirst("p.description")?.text().orEmpty())
            .setContentType("text/html")
            .setRawContent(ByteString.copyFromUtf8(element.outerHtml()))
            .build()
}
