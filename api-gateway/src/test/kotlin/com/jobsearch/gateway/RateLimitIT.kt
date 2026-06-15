package com.jobsearch.gateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Verifies the in-process limiter rejects past the per-period budget. A budget of one and a long
 * refresh window make the second request from the same client deterministically `429`.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = [
        "gateway.rate-limit.limit-for-period=1",
        "gateway.rate-limit.refresh-period=1m",
    ],
)
class RateLimitIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `returns 429 once the per-client budget is exhausted`() {
        offerService.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        client
            .get()
            .uri("/api/v1/offers")
            .exchange()
            .expectStatus()
            .isOk

        client
            .get()
            .uri("/api/v1/offers")
            .exchange()
            .expectStatus()
            .isEqualTo(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader()
            .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
    }

    companion object {
        private val offerService = WireMockServer(options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun startStub() {
            offerService.start()
        }

        @AfterAll
        @JvmStatic
        fun stopStub() {
            offerService.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun routeProperties(registry: DynamicPropertyRegistry) {
            registry.add("OFFER_SERVICE_URI") { offerService.baseUrl() }
        }
    }
}
