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
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * BFF security rules (ADR 0004): the public browse surface stays open, the protected user surface
 * redirects an unauthenticated browser into the OAuth2 login flow, and a CSRF cookie is issued for
 * the SPA. Keycloak's discovery doc is stubbed with WireMock (no real IdP) — the real authorization
 * redirect against Keycloak is covered by [GatewayKeycloakDiscoveryIT].
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayBffSecurityIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `passes a public browse request through unauthenticated`() {
        downstream.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        client
            .get()
            .uri("/api/v1/offers")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `issues an XSRF-TOKEN cookie the SPA can read`() {
        downstream.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        client
            .get()
            .uri("/api/v1/offers")
            .exchange()
            .expectStatus()
            .isOk
            .expectCookie()
            .exists("XSRF-TOKEN")
    }

    @Test
    fun `redirects an unauthenticated request for a protected route into the OAuth2 login`() {
        client
            .get()
            .uri("/api/v1/users/me")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueMatches("Location", ".*/oauth2/authorization/keycloak")
    }

    companion object {
        private val downstream = WireMockServer(options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun startStub() {
            downstream.start()
            OidcDiscoveryStub.register(downstream)
        }

        @AfterAll
        @JvmStatic
        fun stopStub() {
            downstream.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("OFFER_SERVICE_URI") { downstream.baseUrl() }
            registry.add("USER_SERVICE_URI") { downstream.baseUrl() }
            registry.add("KEYCLOAK_ISSUER_URI") { "${downstream.baseUrl()}/realms/job-search" }
        }
    }
}
