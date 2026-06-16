package com.jobsearch.gateway

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Proves the BFF OAuth2 client wires up against a real Keycloak: the committed realm import + the
 * unified-issuer discovery + the `web-bff` registration are consistent, so hitting the authorization
 * endpoint produces a redirect to Keycloak's real `auth` endpoint for our client. (Full interactive
 * login is covered by the Playwright e2e in §2.5.) Redis backs the session that stores the saved
 * authorization request.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayKeycloakDiscoveryIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `redirects to the real Keycloak authorization endpoint for the web-bff client`() {
        client
            .get()
            .uri("/oauth2/authorization/keycloak")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueMatches(
                "Location",
                "${keycloak.authServerUrl}/realms/job-search/protocol/openid-connect/auth\\?.*client_id=web-bff.*",
            )
    }

    companion object {
        @Container
        @JvmStatic
        val keycloak: KeycloakContainer =
            KeycloakContainer("quay.io/keycloak/keycloak:26.1")
                .withRealmImportFile("keycloak/job-search-realm.json")

        @Container
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer("redis:7-alpine").withExposedPorts(REDIS_PORT)

        private const val REDIS_PORT = 6379

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("KEYCLOAK_ISSUER_URI") { "${keycloak.authServerUrl}/realms/job-search" }
            registry.add("REDIS_HOST") { redis.host }
            registry.add("REDIS_PORT") { redis.getMappedPort(REDIS_PORT) }
            registry.add("OFFER_SERVICE_URI") { "http://localhost:1" }
            registry.add("USER_SERVICE_URI") { "http://localhost:1" }
        }
    }
}
