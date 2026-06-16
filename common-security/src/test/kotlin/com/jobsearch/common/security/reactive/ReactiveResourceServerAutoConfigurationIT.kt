package com.jobsearch.common.security.reactive

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Slice test for the reactive resource-server chain — the WebFlux counterpart of
 * [com.jobsearch.common.security.servlet.ServletResourceServerAutoConfigurationIT]: a valid JWT maps
 * realm roles to authorities, a missing/invalid token is rejected `401`, public paths stay open.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.main.web-application-type=reactive",
        "jobsearch.security.public-paths=/public/**",
    ],
)
class ReactiveResourceServerAutoConfigurationIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `rejects a protected request with no token`() {
        client
            .get()
            .uri("/api/me/roles")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `rejects a protected request with an invalid or expired token`() {
        client
            .get()
            .uri("/api/me/roles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer expired")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `accepts a valid token and maps realm roles to authorities`() {
        val body =
            client
                .get()
                .uri("/api/me/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ReactiveResourceServerTestApp.VALID_TOKEN}")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

        body!! shouldContain "ROLE_user"
        body shouldContain "ROLE_admin"
    }

    @Test
    fun `leaves configured public paths open`() {
        client
            .get()
            .uri("/public/ping")
            .exchange()
            .expectStatus()
            .isOk
    }
}
