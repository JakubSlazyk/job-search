package com.jobsearch.collection.source

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
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
import tools.jackson.databind.json.JsonMapper

class HttpJsonOfferSourceTest :
    StringSpec({
        "fetches JSON and maps each element to a uniform RawOffer" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val json =
                    """
                    [
                      {"id":"a1","title":"Kotlin Dev","company":"ACME","url":"https://x/1","location":"Remote","description":"d"}
                    ]
                    """.trimIndent()
                server.stubFor(get(urlEqualTo("/api/offers")).willReturn(okJson(json)))

                val offers = httpJsonSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].source shouldBe "http-json"
                offers[0].externalId shouldBe "a1"
                offers[0].title shouldBe "Kotlin Dev"
                offers[0].contentType shouldBe "application/json"
                offers[0].rawContent.isEmpty shouldBe false
            } finally {
                server.stop()
            }
        }

        "retries a transient 5xx and then succeeds" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                server.stubFor(
                    get(urlEqualTo("/api/offers"))
                        .inScenario("retry")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(serverError())
                        .willSetStateTo("recovered"),
                )
                server.stubFor(
                    get(urlEqualTo("/api/offers"))
                        .inScenario("retry")
                        .whenScenarioStateIs("recovered")
                        .willReturn(okJson("[]")),
                )

                val offers = httpJsonSource(server.baseUrl()).fetch().collectList().block()!!

                offers.shouldBeEmpty()
                server.verify(2, getRequestedFor(urlEqualTo("/api/offers")))
            } finally {
                server.stop()
            }
        }
    })

private fun httpJsonSource(baseUrl: String): HttpJsonOfferSource =
    HttpJsonOfferSource(
        WebClient.builder().build(),
        CollectionSourceProperties(httpJsonUrl = "$baseUrl/api/offers"),
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
        JsonMapper.builder().build(),
    )
