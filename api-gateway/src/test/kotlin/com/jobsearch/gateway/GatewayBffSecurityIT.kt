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
 * BFF security rules (ADR 0004): the public browse surface stays open, an unauthenticated SPA probe
 * under the `/api/` prefix gets a clean `401` (so the SPA derives anonymous state without a redirect
 * chase), a top-level browser navigation to a protected page redirects into the OAuth2 login, and a
 * CSRF cookie is issued for the SPA. Keycloak's discovery doc is stubbed with WireMock (no real IdP)
 * — the real authorization redirect against Keycloak is covered by [GatewayKeycloakDiscoveryIT].
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
    fun `returns 401 for an unauthenticated SPA probe of a protected api route`() {
        client
            .get()
            .uri("/api/v1/users/me")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `redirects an unauthenticated browser navigation to a protected page into the OAuth2 login`() {
        client
            .get()
            .uri("/profile")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueMatches("Location", ".*/oauth2/authorization/keycloak")
    }

    @Test
    fun `rejects a mutating request to a protected route without a CSRF token`() {
        client
            .post()
            .uri("/api/v1/users/me")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `accepts the raw XSRF-TOKEN cookie echoed as the header (SPA pattern, not masked)`() {
        downstream.stubFor(get(urlEqualTo("/api/v1/offers")).willReturn(okJson("[]")))

        // The SPA reads the raw XSRF-TOKEN cookie and echoes it as the X-XSRF-TOKEN header. With the
        // reactive default (masking) handler this raw value would be rejected (403); the plain request
        // handler resolves it, so CSRF passes and the request proceeds to authentication — which, for
        // an unauthenticated /api call, is the 401 from the api-aware entry point (not a 403).
        val token =
            client
                .get()
                .uri("/api/v1/offers")
                .exchange()
                .expectStatus()
                .isOk
                .returnResult(String::class.java)
                .responseCookies
                .getFirst("XSRF-TOKEN")!!
                .value

        client
            .post()
            .uri("/api/v1/users/me")
            .cookie("XSRF-TOKEN", token)
            .header("X-XSRF-TOKEN", token)
            .exchange()
            .expectStatus()
            .isUnauthorized
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
