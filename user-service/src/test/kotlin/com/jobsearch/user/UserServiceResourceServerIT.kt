package com.jobsearch.user

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Resource-server + CRUD slice over a real Postgres (Flyway runs the migrations). Asserts the §2.0
 * servlet chain protects /api/v1/users/me (401 without a token), that a valid Keycloak-shaped JWT
 * provisions the user, and that profile/preferences round-trip. The JWT is injected via
 * spring-security-test, so no Keycloak is needed — real validation is covered by the gateway BFF IT.
 * The embedded gRPC server is pinned to a random port so parallel/repeated contexts don't fight 9090.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = ["spring.grpc.server.port=0"])
@AutoConfigureMockMvc
class UserServiceResourceServerIT {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbc: JdbcClient

    @Test
    fun `rejects an unauthenticated request`() {
        mockMvc.get("/api/v1/users/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `serves the subject and provisions the user for a valid jwt`() {
        mockMvc
            .get("/api/v1/users/me") {
                with(
                    jwt().jwt { builder ->
                        builder
                            .subject("user-123")
                            .claim("preferred_username", "tester")
                            .claim("email", "tester@example.com")
                    },
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.subject") { value("user-123") }
                jsonPath("$.username") { value("tester") }
                jsonPath("$.email") { value("tester@example.com") }
            }

        val count =
            jdbc
                .sql("SELECT count(*) FROM users WHERE subject = 'user-123'")
                .query(Long::class.javaObjectType)
                .single()
        count shouldBe 1L
    }

    @Test
    fun `updates the profile and reads it back`() {
        val token =
            jwt().jwt {
                it
                    .subject(
                        "user-profile",
                    ).claim("preferred_username", "profileuser")
                    .claim("email", "p@example.com")
            }

        mockMvc
            .put("/api/v1/users/me/profile") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content =
                    """{"displayName":"Test User","headline":"Engineer","location":"Warsaw","linkedinUrl":"https://www.linkedin.com/in/test"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.displayName") { value("Test User") }
            }

        mockMvc
            .get("/api/v1/users/me") { with(token) }
            .andExpect {
                status { isOk() }
                jsonPath("$.displayName") { value("Test User") }
                jsonPath("$.headline") { value("Engineer") }
                jsonPath("$.location") { value("Warsaw") }
                jsonPath("$.preferences.emailNotificationsEnabled") { value(true) }
            }
    }

    @Test
    fun `updates preferences`() {
        val token = jwt().jwt { it.subject("user-prefs").claim("preferred_username", "prefuser") }

        mockMvc
            .put("/api/v1/users/me/preferences") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"emailNotificationsEnabled":false,"locale":"pl"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.preferences.emailNotificationsEnabled") { value(false) }
                jsonPath("$.preferences.locale") { value("pl") }
            }
    }

    @Test
    fun `rejects an invalid profile payload with 400`() {
        val token = jwt().jwt { it.subject("user-invalid").claim("preferred_username", "invaliduser") }

        mockMvc
            .put("/api/v1/users/me/profile") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"linkedinUrl":"not-a-valid-url"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    /**
     * Stub decoder so the context starts without reaching Keycloak; the spring-security-test `jwt()`
     * post-processor establishes the authentication directly, so this decoder is never invoked.
     */
    @TestConfiguration
    class StubJwtDecoderConfiguration {
        @Bean
        fun jwtDecoder(): JwtDecoder = JwtDecoder { error("not used in slice tests") }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("users")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
