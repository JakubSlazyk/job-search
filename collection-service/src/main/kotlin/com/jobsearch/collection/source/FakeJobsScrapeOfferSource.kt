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
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

/**
 * Scrape source for the RealPython "fake-jobs" board (`https://realpython.github.io/fake-jobs/`), a
 * static, scrape-friendly HTML page. Its card markup differs from the generic [HtmlScrapeOfferSource],
 * so it gets its own anti-corruption adapter (ADR 0005): it fetches the page via [WebClient]
 * (non-blocking) and extracts each job card with [Jsoup], retaining the card's original HTML as
 * `raw_content`. Descriptions live on per-job detail pages and are not fetched here (card-only —
 * detail enrichment deferred). The outbound call is guarded by Resilience4j retry + circuit-breaker.
 * No-ops when its URL is unconfigured.
 */
@Component
class FakeJobsScrapeOfferSource(
    private val webClient: WebClient,
    private val properties: CollectionSourceProperties,
    retryRegistry: RetryRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) : OfferSource {
    override val sourceName = "fake-jobs"
    private val retry = retryRegistry.retry(sourceName)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(sourceName)

    override fun fetch(): Flux<RawOffer> {
        val url = properties.fakeJobsUrl
        if (url.isBlank()) return Flux.empty()
        return webClient
            .get()
            .uri(url)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .flatMapMany { html -> Flux.fromIterable(parse(html)) }
    }

    private fun parse(html: String): List<RawOffer> =
        Jsoup.parse(html).select("div.card-content").mapNotNull(::toRawOffer)

    /** Maps one job card, or null when it has no apply link to derive a stable id from. */
    private fun toRawOffer(card: Element): RawOffer? {
        val applyUrl = card.selectFirst("a.card-footer-item:contains(Apply)")?.attr("href").orEmpty()
        val externalId = externalIdOf(applyUrl)
        if (externalId.isBlank()) return null
        return RawOffer
            .newBuilder()
            .setSource(sourceName)
            .setExternalId(externalId)
            .setFetchedAt(timestampOf())
            .setTitle(card.selectFirst("h2.title")?.text().orEmpty())
            .setCompany(card.selectFirst("h3.subtitle.company")?.text().orEmpty())
            .setUrl(applyUrl)
            .setLocation(card.selectFirst("p.location")?.text().orEmpty())
            // description omitted: not present on the listing card (detail-page enrichment deferred).
            .setContentType("text/html")
            .setRawContent(ByteString.copyFromUtf8(card.outerHtml()))
            .build()
    }

    /** Clean, path-safe id from the apply URL slug (drops the host path and the `.html` suffix). */
    private fun externalIdOf(applyUrl: String): String = applyUrl.substringAfterLast('/').removeSuffix(".html")

    private companion object {
        const val USER_AGENT = "job-search-collection-service (+https://github.com/jakubslazyk/job-search)"
    }
}
