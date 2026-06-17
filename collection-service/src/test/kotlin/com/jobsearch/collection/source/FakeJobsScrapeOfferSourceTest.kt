package com.jobsearch.collection.source

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.jobsearch.collection.CollectionSourceProperties
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.web.reactive.function.client.WebClient

class FakeJobsScrapeOfferSourceTest :
    StringSpec({
        "scrapes fake-jobs cards and maps each to a uniform RawOffer" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                server.stubFor(html("/", twoCardsHtml))

                val offers = fakeJobsSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 2
                offers[0].source shouldBe "fake-jobs"
                offers[0].externalId shouldBe "senior-python-developer-0"
                offers[0].title shouldBe "Senior Python Developer"
                offers[0].company shouldBe "Payne, Roberts and Davis"
                offers[0].url shouldBe "https://realpython.github.io/fake-jobs/jobs/senior-python-developer-0.html"
                offers[0].location shouldBe "Stewartbury, AA"
                offers[0].contentType shouldBe "text/html"
                offers[0].rawContent.isEmpty shouldBe false
                offers[1].externalId shouldBe "energy-engineer-1"
            } finally {
                server.stop()
            }
        }

        "skips a card with no apply link" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val html =
                    """
                    <html><body>
                      <div class="card-content">
                        <h2 class="title is-5">No Apply Link</h2>
                        <h3 class="subtitle is-6 company">Nowhere Inc</h3>
                        <footer class="card-footer">
                          <a href="https://x/learn" class="card-footer-item">Learn</a>
                        </footer>
                      </div>
                    </body></html>
                    """.trimIndent()
                server.stubFor(html("/", html))

                val offers = fakeJobsSource(server.baseUrl()).fetch().collectList().block()!!

                offers.shouldBeEmpty()
            } finally {
                server.stop()
            }
        }

        "retries a transient 5xx and then succeeds" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                server.stubFor(
                    get(urlEqualTo("/"))
                        .inScenario("retry")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(serverError())
                        .willSetStateTo("recovered"),
                )
                server.stubFor(
                    get(urlEqualTo("/"))
                        .inScenario("retry")
                        .whenScenarioStateIs("recovered")
                        .willReturn(aResponse().withHeader("Content-Type", "text/html").withBody("<html></html>")),
                )

                val offers = fakeJobsSource(server.baseUrl()).fetch().collectList().block()!!

                offers.shouldBeEmpty()
                server.verify(2, getRequestedFor(urlEqualTo("/")))
            } finally {
                server.stop()
            }
        }
    })

private fun html(
    path: String,
    body: String,
) = get(urlEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(body))

private fun fakeJobsSource(baseUrl: String): FakeJobsScrapeOfferSource =
    FakeJobsScrapeOfferSource(
        WebClient.builder().build(),
        CollectionSourceProperties(fakeJobsUrl = baseUrl),
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
    )

private val twoCardsHtml =
    """
    <html><body>
      <div class="card">
        <div class="card-content">
          <div class="media"><div class="media-content">
            <h2 class="title is-5">Senior Python Developer</h2>
            <h3 class="subtitle is-6 company">Payne, Roberts and Davis</h3>
          </div></div>
          <div class="content">
            <p class="location"> Stewartbury, AA </p>
            <p class="is-small has-text-grey"><time datetime="2021-04-08">2021-04-08</time></p>
          </div>
          <footer class="card-footer">
            <a href="https://www.realpython.com" class="card-footer-item">Learn</a>
            <a href="https://realpython.github.io/fake-jobs/jobs/senior-python-developer-0.html" class="card-footer-item">Apply</a>
          </footer>
        </div>
      </div>
      <div class="card">
        <div class="card-content">
          <div class="media"><div class="media-content">
            <h2 class="title is-5">Energy engineer</h2>
            <h3 class="subtitle is-6 company">Vasquez-Davidson</h3>
          </div></div>
          <div class="content">
            <p class="location"> Christopherville, AA </p>
            <p class="is-small has-text-grey"><time datetime="2021-04-08">2021-04-08</time></p>
          </div>
          <footer class="card-footer">
            <a href="https://www.realpython.com" class="card-footer-item">Learn</a>
            <a href="https://realpython.github.io/fake-jobs/jobs/energy-engineer-1.html" class="card-footer-item">Apply</a>
          </footer>
        </div>
      </div>
    </body></html>
    """.trimIndent()
