package com.jobsearch.collection.source

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.jobsearch.collection.CollectionSourceProperties
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.web.reactive.function.client.WebClient

class HtmlScrapeOfferSourceTest :
    StringSpec({
        "scrapes HTML and maps each offer element to a uniform RawOffer" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val html =
                    """
                    <html><body>
                      <div class="offer" data-id="x1">
                        <h2 class="title">Backend Engineer</h2>
                        <span class="company">Globex</span>
                        <a class="link" href="https://x/9">apply</a>
                        <span class="location">Berlin</span>
                        <p class="description">desc</p>
                      </div>
                    </body></html>
                    """.trimIndent()
                server.stubFor(
                    get(urlEqualTo("/offers.html"))
                        .willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(html)),
                )

                val offers = htmlScrapeSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].source shouldBe "html-scrape"
                offers[0].externalId shouldBe "x1"
                offers[0].title shouldBe "Backend Engineer"
                offers[0].company shouldBe "Globex"
                offers[0].url shouldBe "https://x/9"
                offers[0].location shouldBe "Berlin"
                offers[0].contentType shouldBe "text/html"
            } finally {
                server.stop()
            }
        }
    })

private fun htmlScrapeSource(baseUrl: String): HtmlScrapeOfferSource =
    HtmlScrapeOfferSource(
        WebClient.builder().build(),
        CollectionSourceProperties(htmlScrapeUrl = "$baseUrl/offers.html"),
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
    )
