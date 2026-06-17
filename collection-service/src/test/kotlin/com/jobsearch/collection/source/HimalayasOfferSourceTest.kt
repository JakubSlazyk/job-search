package com.jobsearch.collection.source

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
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

class HimalayasOfferSourceTest :
    StringSpec({
        "fetches the Himalayas jobs object and maps each job to a uniform RawOffer" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val json =
                    """
                    {
                      "totalCount": 1,
                      "jobs": [
                        {
                          "guid": "https://himalayas.app/companies/acme/jobs/kotlin-dev-12345",
                          "title": "Kotlin Dev",
                          "companyName": "ACME",
                          "applicationLink": "https://himalayas.app/apply/12345",
                          "locationRestrictions": ["USA", "Canada"],
                          "description": "<p>We are hiring.</p>"
                        }
                      ]
                    }
                    """.trimIndent()
                server.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson(json)))

                val offers = himalayasSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].source shouldBe "himalayas"
                offers[0].externalId shouldBe "12345"
                offers[0].title shouldBe "Kotlin Dev"
                offers[0].company shouldBe "ACME"
                offers[0].url shouldBe "https://himalayas.app/apply/12345"
                offers[0].location shouldBe "USA, Canada"
                offers[0].description shouldBe "<p>We are hiring.</p>"
                offers[0].contentType shouldBe "application/json"
                offers[0].rawContent.isEmpty shouldBe false
            } finally {
                server.stop()
            }
        }

        "sends the configured limit as a query parameter" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                server.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson("""{"jobs":[]}""")))

                himalayasSource(server.baseUrl()).fetch().collectList().block()

                server.verify(getRequestedFor(urlPathEqualTo("/jobs/api")).withQueryParam("limit", equalTo("50")))
            } finally {
                server.stop()
            }
        }

        "joins locationRestrictions and falls back to Worldwide when empty" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val json =
                    """
                    {"jobs":[{"guid":"https://himalayas.app/jobs/dev-1","title":"t","companyName":"c","applicationLink":"u","locationRestrictions":[]}]}
                    """.trimIndent()
                server.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson(json)))

                val offers = himalayasSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].location shouldBe "Worldwide"
            } finally {
                server.stop()
            }
        }

        "skips jobs with a blank guid" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val json =
                    """
                    {"jobs":[
                      {"guid":"","title":"no id","companyName":"c","applicationLink":"u"},
                      {"guid":"https://himalayas.app/jobs/keep-77","title":"keep","companyName":"c","applicationLink":"u"}
                    ]}
                    """.trimIndent()
                server.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson(json)))

                val offers = himalayasSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].externalId shouldBe "77"
            } finally {
                server.stop()
            }
        }

        "falls back to the slug when the guid has no trailing numeric id" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                val json =
                    """
                    {"jobs":[{"guid":"https://himalayas.app/jobs/no-numeric-id","title":"t","companyName":"c","applicationLink":"u"}]}
                    """.trimIndent()
                server.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson(json)))

                val offers = himalayasSource(server.baseUrl()).fetch().collectList().block()!!

                offers.size shouldBe 1
                offers[0].externalId shouldBe "no-numeric-id"
            } finally {
                server.stop()
            }
        }

        "retries a transient 5xx and then succeeds" {
            val server = WireMockServer(options().dynamicPort())
            server.start()
            try {
                server.stubFor(
                    get(urlPathEqualTo("/jobs/api"))
                        .inScenario("retry")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(serverError())
                        .willSetStateTo("recovered"),
                )
                server.stubFor(
                    get(urlPathEqualTo("/jobs/api"))
                        .inScenario("retry")
                        .whenScenarioStateIs("recovered")
                        .willReturn(okJson("""{"jobs":[]}""")),
                )

                val offers = himalayasSource(server.baseUrl()).fetch().collectList().block()!!

                offers.shouldBeEmpty()
                server.verify(2, getRequestedFor(urlPathEqualTo("/jobs/api")))
            } finally {
                server.stop()
            }
        }
    })

private fun himalayasSource(baseUrl: String): HimalayasOfferSource =
    HimalayasOfferSource(
        WebClient.builder().build(),
        CollectionSourceProperties(himalayasUrl = "$baseUrl/jobs/api"),
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
        JsonMapper.builder().build(),
    )
