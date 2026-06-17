package com.jobsearch.collection

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.jobsearch.common.domain.Topics
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/**
 * End-to-end emit path: POST /api/v1/collect runs every source and publishes a RawOffer per offer to
 * `raw-offers`, keyed by `source:externalId`. The two real adapters are exercised against WireMock
 * (captured real-shaped fixtures — CI never hits the live endpoints). Verifies both sources publish,
 * a failing source is isolated, and re-collection yields the same identities (no duplicates).
 * Skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionFlowIT {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @BeforeEach
    fun resetStubs() {
        wireMock.resetAll()
    }

    @Test
    fun `collect publishes offers from both real sources (and the sample) to raw-offers`() {
        stubHimalayas()
        stubFakeJobs()

        val consumer = consumerAtEnd()
        collect()
        val keys = poll(consumer, minRecords = 4).map { it.key() }.toSet()
        consumer.close()

        assertTrue(keys.containsAll(EXPECTED_KEYS)) { "expected $EXPECTED_KEYS, got $keys" }
    }

    @Test
    fun `a failing source is isolated - the healthy source still publishes`() {
        wireMock.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(serverError()))
        stubFakeJobs()

        val consumer = consumerAtEnd()
        collect()
        val keys = poll(consumer, minRecords = 3).map { it.key() }.toSet()
        consumer.close()

        assertTrue(keys.containsAll(FAKE_JOBS_KEYS + SAMPLE_KEY)) { "healthy sources missing: $keys" }
        assertFalse(keys.contains(HIMALAYAS_KEY)) { "failing source should not have published: $keys" }
    }

    @Test
    fun `re-collection yields the same identities (no duplicates)`() {
        stubHimalayas()
        stubFakeJobs()

        val consumer = consumerAtEnd()
        collect()
        val firstRun = poll(consumer, minRecords = 4).map { it.key() }.toSet()
        collect()
        val secondRun = poll(consumer, minRecords = 4).map { it.key() }.toSet()
        consumer.close()

        // Identity is `source + externalId`, so a second run republishes the same keys, not new ones.
        assertEquals(firstRun, secondRun)
        assertTrue(firstRun.containsAll(EXPECTED_KEYS))
    }

    private fun collect() {
        HttpClient.newHttpClient().send(
            HttpRequest
                .newBuilder(URI.create("http://localhost:$port/api/v1/collect"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.discarding(),
        )
    }

    /** A fresh consumer positioned at the end of `raw-offers`, so it reads only offers from this point on. */
    private fun consumerAtEnd(): Consumer<String, ByteArray> {
        val consumer =
            DefaultKafkaConsumerFactory(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ),
                StringDeserializer(),
                ByteArrayDeserializer(),
            ).createConsumer("collection-it-${UUID.randomUUID()}", "")
        consumer.subscribe(listOf(Topics.RAW_OFFERS))
        val deadline = System.currentTimeMillis() + 10_000
        while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(100))
        }
        consumer.seekToEnd(consumer.assignment())
        consumer.assignment().forEach { consumer.position(it) }
        return consumer
    }

    private fun poll(
        consumer: Consumer<String, ByteArray>,
        minRecords: Int,
    ): List<ConsumerRecord<String, ByteArray>> =
        KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15), minRecords).records(Topics.RAW_OFFERS).toList()

    private fun stubHimalayas() {
        wireMock.stubFor(get(urlPathEqualTo("/jobs/api")).willReturn(okJson(HIMALAYAS_JSON)))
    }

    private fun stubFakeJobs() {
        wireMock.stubFor(
            get(urlEqualTo("/fake-jobs"))
                .willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(FAKE_JOBS_HTML)),
        )
    }

    companion object {
        @JvmStatic
        val wireMock: WireMockServer = WireMockServer(options().dynamicPort()).apply { start() }

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))

        @JvmStatic
        @AfterAll
        fun stopWireMock() = wireMock.stop()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("collection.sources.himalayas-url") { "${wireMock.baseUrl()}/jobs/api" }
            registry.add("collection.sources.fake-jobs-url") { "${wireMock.baseUrl()}/fake-jobs" }
        }

        private const val SAMPLE_KEY = "sample:offer-1"
        private const val HIMALAYAS_KEY = "himalayas:12345"
        private val FAKE_JOBS_KEYS = setOf("fake-jobs:senior-python-developer-0", "fake-jobs:energy-engineer-1")
        private val EXPECTED_KEYS = FAKE_JOBS_KEYS + HIMALAYAS_KEY + SAMPLE_KEY

        private val HIMALAYAS_JSON =
            """
            {"jobs":[{
              "guid":"https://himalayas.app/companies/acme/jobs/kotlin-dev-12345",
              "title":"Kotlin Dev","companyName":"ACME",
              "applicationLink":"https://himalayas.app/apply/12345",
              "locationRestrictions":["USA"],"description":"<p>We are hiring.</p>"
            }]}
            """.trimIndent()

        private val FAKE_JOBS_HTML =
            """
            <html><body>
              <div class="card"><div class="card-content">
                <div class="media"><div class="media-content">
                  <h2 class="title is-5">Senior Python Developer</h2>
                  <h3 class="subtitle is-6 company">Payne, Roberts and Davis</h3>
                </div></div>
                <div class="content"><p class="location"> Stewartbury, AA </p></div>
                <footer class="card-footer">
                  <a href="https://realpython.github.io/fake-jobs/jobs/senior-python-developer-0.html" class="card-footer-item">Apply</a>
                </footer>
              </div></div>
              <div class="card"><div class="card-content">
                <div class="media"><div class="media-content">
                  <h2 class="title is-5">Energy engineer</h2>
                  <h3 class="subtitle is-6 company">Vasquez-Davidson</h3>
                </div></div>
                <div class="content"><p class="location"> Christopherville, AA </p></div>
                <footer class="card-footer">
                  <a href="https://realpython.github.io/fake-jobs/jobs/energy-engineer-1.html" class="card-footer-item">Apply</a>
                </footer>
              </div></div>
            </body></html>
            """.trimIndent()
    }
}
