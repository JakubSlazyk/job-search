package com.jobsearch.offer.search

import com.jobsearch.offer.Offer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.RefreshRequest
import org.opensearch.client.transport.OpenSearchTransport
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * End-to-end read-model test against a real OpenSearch (Testcontainers). It builds the production
 * [OpenSearchConfig] transport, creates the index via [OfferIndexInitializer], indexes offers and
 * asserts full-text search / keyword filtering — the real HTTP round-trip the mocked
 * [OfferSearchIndexTest] cannot cover, so transport regressions (e.g. response-compression handling)
 * surface here. Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class OfferSearchIndexIT {
    @Test
    fun `full-text query matches analyzed title fields across documents`() {
        assertEquals(
            listOf("kt:1", "kt:2"),
            index.search(OfferSearchCriteria(query = "kotlin")).map { it.offerId }.sorted(),
        )
    }

    @Test
    fun `keyword filters match exactly and combine with the query`() {
        assertEquals(
            listOf("kt:1", "kt:2"),
            index.search(OfferSearchCriteria(seniority = "SENIOR")).map { it.offerId }.sorted(),
        )
        assertEquals(
            listOf("kt:1"),
            index.search(OfferSearchCriteria(query = "kotlin", location = "Warsaw")).map { it.offerId },
        )
    }

    @Test
    fun `blank criteria browses every document`() {
        assertEquals(3, index.search(OfferSearchCriteria()).size)
    }

    @Test
    fun `findById returns the indexed offer and null when absent`() {
        assertEquals("ACME", index.findById("kt:1")?.company)
        assertNull(index.findById("does-not-exist"))
    }

    companion object {
        @Container
        @JvmStatic
        val opensearch: GenericContainer<*> =
            GenericContainer<Nothing>(DockerImageName.parse("opensearchproject/opensearch:2.19.0")).apply {
                withExposedPorts(9200)
                withEnv("discovery.type", "single-node")
                withEnv("DISABLE_SECURITY_PLUGIN", "true")
                withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                waitingFor(
                    Wait.forHttp("/_cluster/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)),
                )
            }

        private lateinit var transport: OpenSearchTransport
        private lateinit var index: OfferSearchIndex

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val properties = OpenSearchProperties("http", opensearch.host, opensearch.getMappedPort(9200))
            transport = OpenSearchConfig().openSearchTransport(properties)
            val client = OpenSearchClient(transport)

            OfferIndexInitializer(client).ensureIndex()
            index = OfferSearchIndex(client)

            listOf(
                Offer(
                    "kt:1",
                    "jb",
                    "1",
                    "Senior Kotlin Engineer",
                    "ACME",
                    "https://x/1",
                    "Warsaw",
                    "Backend role",
                    "SENIOR",
                ),
                Offer(
                    "kt:2",
                    "jb",
                    "2",
                    "Kotlin Backend Developer",
                    "Globex",
                    "https://x/2",
                    "Remote",
                    "Services",
                    "SENIOR",
                ),
                Offer("jv:1", "jb", "3", "Java Developer", "Initech", "https://x/3", "Remote", "Legacy code", "MID"),
            ).forEach(index::index)

            // Make the freshly indexed docs visible to search without waiting for the default refresh.
            client.indices().refresh(RefreshRequest.Builder().index(OFFERS_INDEX).build())
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            transport.close()
        }
    }
}
