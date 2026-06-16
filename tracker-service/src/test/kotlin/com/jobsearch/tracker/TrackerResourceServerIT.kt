package com.jobsearch.tracker

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Resource-server + CRUD slice over a real Postgres (Flyway runs the migrations). Asserts the §2.0
 * servlet chain protects the tracker routes (401 without a token), the full track/read/update/untrack
 * round-trip, an un-snapshotted offer renders with a null `offer` (enrich-later), and that an unknown
 * status value is a 400. The JWT is injected via spring-security-test, so no Keycloak is needed. The
 * Kafka listener is disabled here — this slice has no broker; the consumer path is covered by
 * OfferPublishedConsumerIT.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = ["spring.kafka.listener.auto-startup=false"])
@AutoConfigureMockMvc
class TrackerResourceServerIT {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `rejects an unauthenticated request`() {
        mockMvc.get("/api/v1/tracker/applications").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `tracks, reads back, updates and untracks an offer`() {
        val token = jwt().jwt { it.subject("user-track") }

        mockMvc
            .post("/api/v1/tracker/applications") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"offerId":"sample:1","status":"APPLIED","notes":"applied today"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.offerId") { value("sample:1") }
                jsonPath("$.status") { value("APPLIED") }
                jsonPath("$.offer") { value(null) } // enrich-later: no snapshot yet
            }

        mockMvc
            .get("/api/v1/tracker/applications") { with(token) }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].offerId") { value("sample:1") }
                jsonPath("$[0].offer") { value(null) }
            }

        mockMvc
            .put("/api/v1/tracker/applications/sample:1") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"status":"REJECTED","notes":"no fit"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("REJECTED") }
            }

        mockMvc.delete("/api/v1/tracker/applications/sample:1") { with(token) }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/v1/tracker/applications/sample:1") { with(token) }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `does not leak another user's tracked offers`() {
        val owner = jwt().jwt { it.subject("owner") }
        val other = jwt().jwt { it.subject("other") }

        mockMvc
            .post("/api/v1/tracker/applications") {
                with(owner)
                contentType = MediaType.APPLICATION_JSON
                content = """{"offerId":"sample:scoped","status":"SAVED","notes":null}"""
            }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/tracker/applications") { with(other) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `rejects an unknown status with 400`() {
        val token = jwt().jwt { it.subject("user-invalid") }

        mockMvc
            .post("/api/v1/tracker/applications") {
                with(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"offerId":"sample:1","status":"NOPE","notes":null}"""
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
        val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("tracker")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
