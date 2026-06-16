package com.jobsearch.notification

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono

/**
 * Reactive resource-server + criteria CRUD slice over a real Postgres (Flyway migrates via JDBC, the
 * app reads/writes via R2DBC). Asserts the §2.0 reactive chain protects the routes (401 without a
 * token), the criteria round-trip, self-scoping, an unknown id 404, and a blank keyword 400. The JWT
 * is injected via spring-security-test, so no Keycloak is needed. The Kafka consumer is disabled —
 * this slice has no broker; the consumer path is covered by OfferPublishedConsumerIT.
 *
 * The WebTestClient is bound to the application context with `springSecurity()` applied — that
 * installs the configurer the `mockJwt()` mutator needs to populate the reactive security context
 * (without it every request reaches the resource server anonymous → 401).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = ["notification.kafka.enabled=false"])
class NotificationResourceServerIT {
    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client =
            WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
    }

    @Test
    fun `rejects an unauthenticated request`() {
        client
            .get()
            .uri("/api/v1/notifications/criteria")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `creates, lists and deletes a criterion`() {
        val user = mockJwt().jwt { it.subject("user-crit") }

        val id =
            client
                .mutateWith(user)
                .post()
                .uri("/api/v1/notifications/criteria")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"keyword":"kotlin"}""")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.keyword")
                .isEqualTo("kotlin")
                .jsonPath("$.id")
                .exists()
                .returnResult()
                .let { String(it.responseBody!!) }
                .substringAfter("\"id\":")
                .substringBefore(",")

        client
            .mutateWith(user)
            .get()
            .uri("/api/v1/notifications/criteria")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(1)
            .jsonPath("$[0].keyword")
            .isEqualTo("kotlin")

        client
            .mutateWith(user)
            .delete()
            .uri("/api/v1/notifications/criteria/$id")
            .exchange()
            .expectStatus()
            .isNoContent

        client
            .mutateWith(user)
            .delete()
            .uri("/api/v1/notifications/criteria/$id")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `does not leak another user's criteria`() {
        val owner = mockJwt().jwt { it.subject("owner") }
        val other = mockJwt().jwt { it.subject("other") }

        client
            .mutateWith(owner)
            .post()
            .uri("/api/v1/notifications/criteria")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"keyword":"scoped"}""")
            .exchange()
            .expectStatus()
            .isOk

        client
            .mutateWith(other)
            .get()
            .uri("/api/v1/notifications/criteria")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(0)
    }

    @Test
    fun `rejects a blank keyword with 400`() {
        client
            .mutateWith(mockJwt().jwt { it.subject("user-blank") })
            .post()
            .uri("/api/v1/notifications/criteria")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"keyword":"  "}""")
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /** Stub decoder so the context starts without Keycloak; the `mockJwt()` configurer is used instead. */
    @TestConfiguration
    class StubReactiveJwtDecoderConfiguration {
        @Bean
        fun reactiveJwtDecoder(): ReactiveJwtDecoder =
            ReactiveJwtDecoder { Mono.error(IllegalStateException("not used in slice tests")) }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("notification")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.registerPostgres(postgres)
        }
    }
}
