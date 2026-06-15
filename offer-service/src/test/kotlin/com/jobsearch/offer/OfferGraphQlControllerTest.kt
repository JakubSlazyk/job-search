package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.graphql.test.tester.GraphQlTester

/**
 * GraphQL slice test (§1.6): boots only the GraphQL infrastructure + [OfferGraphQlController] and
 * drives it through [GraphQlTester], so it verifies the schema binds to the controller and arguments
 * map correctly. The read model is mocked via a nested [TestConfiguration].
 */
@GraphQlTest(OfferGraphQlController::class)
class OfferGraphQlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Autowired
    private lateinit var searchIndex: OfferSearchIndex

    @Test
    fun `offers query maps arguments and returns hits`() {
        val criteria = slot<OfferSearchCriteria>()
        every { searchIndex.search(capture(criteria)) } returns listOf(sample)

        graphQlTester
            .document("{ offers(query: \"kotlin\", seniority: \"SENIOR\", page: 2, size: 10) { offerId title } }")
            .execute()
            .path("offers[0].offerId")
            .entity(String::class.java)
            .isEqualTo("sample:1")

        assert(criteria.captured.query == "kotlin")
        assert(criteria.captured.seniority == "SENIOR")
        assert(criteria.captured.from == 20) // page 2 * size 10
        assert(criteria.captured.size == 10)
    }

    @Test
    fun `offer query returns a single offer by id`() {
        every { searchIndex.findById("sample:1") } returns sample

        graphQlTester
            .document("{ offer(offerId: \"sample:1\") { company } }")
            .execute()
            .path("offer.company")
            .entity(String::class.java)
            .isEqualTo("ACME")
    }

    @Test
    fun `offer query returns null when absent`() {
        every { searchIndex.findById("missing") } returns null

        graphQlTester
            .document("{ offer(offerId: \"missing\") { company } }")
            .execute()
            .path("offer")
            .valueIsNull()
    }

    @TestConfiguration
    class MockSearchIndexConfig {
        @Bean
        fun searchIndex(): OfferSearchIndex = mockk()
    }

    private companion object {
        val sample =
            Offer(
                offerId = "sample:1",
                source = "sample",
                externalId = "1",
                title = "Engineer",
                company = "ACME",
                url = "https://example.com/1",
                location = "Remote",
                description = "desc",
                seniority = "SENIOR",
            )
    }
}
