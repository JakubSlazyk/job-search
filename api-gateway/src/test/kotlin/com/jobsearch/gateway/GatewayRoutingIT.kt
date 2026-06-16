package com.jobsearch.gateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.jobsearch.common.web.Correlation
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * End-to-end routing through the gateway with offer-service stubbed by WireMock: verifies REST and
 * GraphQL paths reach the downstream and that the correlation id is minted/echoed/propagated.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @BeforeEach
    fun resetStubs() {
        offerService.resetAll()
    }

    @Test
    fun `routes REST to offer-service and forwards a minted correlation id`() {
        offerService.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        client
            .get()
            .uri("/api/v1/offers")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .exists(Correlation.HEADER)

        offerService.verify(
            getRequestedFor(urlEqualTo("/api/v1/offers"))
                .withHeader(Correlation.HEADER, matching(".+")),
        )
    }

    @Test
    fun `reuses an inbound correlation id end-to-end`() {
        offerService.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        client
            .get()
            .uri("/api/v1/offers")
            .header(Correlation.HEADER, "trace-123")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals(Correlation.HEADER, "trace-123")

        offerService.verify(
            getRequestedFor(urlEqualTo("/api/v1/offers"))
                .withHeader(Correlation.HEADER, equalTo("trace-123")),
        )
    }

    @Test
    fun `routes GraphQL queries to offer-service`() {
        offerService.stubFor(post(urlEqualTo("/graphql")).willReturn(okJson("""{"data":{"__typename":"Query"}}""")))

        client
            .post()
            .uri("/graphql")
            .bodyValue("""{"query":"{ __typename }"}""")
            .exchange()
            .expectStatus()
            .isOk
    }

    companion object {
        private val offerService = WireMockServer(options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun startStub() {
            offerService.start()
            OidcDiscoveryStub.register(offerService)
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
            // Build the OAuth2 client registration against the stubbed discovery doc (no real Keycloak).
            registry.add("KEYCLOAK_ISSUER_URI") { "${offerService.baseUrl()}/realms/job-search" }
        }
    }
}
