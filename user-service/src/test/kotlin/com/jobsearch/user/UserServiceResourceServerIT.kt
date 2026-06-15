package com.jobsearch.user

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Resource-server slice over a real Postgres (Flyway runs the users migration). Asserts the §2.0
 * servlet chain protects /api/v1/users/me (401 without a token), and that a valid Keycloak-shaped JWT
 * is served and provisions the user row. The JWT is injected via spring-security-test, so no Keycloak
 * is needed here — real JWT validation against Keycloak is exercised by the gateway BFF IT.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
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
