package com.jobsearch.offer.search

import com.jobsearch.offer.Offer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.GetRequest
import org.opensearch.client.opensearch.core.GetResponse
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.IndexResponse
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.opensearch.core.search.HitsMetadata

private fun offer(id: String) = Offer(id, "sample", "1", "Engineer", "ACME", "https://x/1", "Remote", "desc", "SENIOR")

private fun document(id: String) =
    OfferDocument().apply {
        offerId = id
        title = "Engineer"
    }

class OfferSearchIndexTest :
    StringSpec({
        val client = mockk<OpenSearchClient>()
        val index = OfferSearchIndex(client)

        "index sends an IndexRequest for the offer" {
            every { client.index(any<IndexRequest<Offer>>()) } returns mockk<IndexResponse>()

            index.index(offer("sample:1"))

            verify { client.index(any<IndexRequest<Offer>>()) }
        }

        "buildQuery uses match_all when blank and adds one filter per provided criterion" {
            index.buildQuery(OfferSearchCriteria()).let {
                it.isBool shouldBe true
                it.bool().must().size shouldBe 1
                it.bool().filter().size shouldBe 0
            }
            val criteria = OfferSearchCriteria(query = "kotlin", source = "s", location = "l", seniority = "SENIOR")
            index.buildQuery(criteria).let {
                it.bool().must().size shouldBe 1
                it.bool().filter().size shouldBe 3
            }
        }

        "search maps hits to offers" {
            val hit = mockk<Hit<OfferDocument>> { every { source() } returns document("sample:1") }
            val hitsMeta = mockk<HitsMetadata<OfferDocument>> { every { hits() } returns listOf(hit) }
            val response = mockk<SearchResponse<OfferDocument>> { every { hits() } returns hitsMeta }
            every { client.search(any<SearchRequest>(), OfferDocument::class.java) } returns response

            index.search(OfferSearchCriteria(query = "x")).map { it.offerId } shouldBe listOf("sample:1")
        }

        "findById returns the mapped offer when found and null when absent" {
            val found = mockk<GetResponse<OfferDocument>> { every { source() } returns document("sample:1") }
            val missing = mockk<GetResponse<OfferDocument>> { every { source() } returns null }
            every { client.get(any<GetRequest>(), OfferDocument::class.java) } returnsMany listOf(found, missing)

            index.findById("sample:1")?.offerId shouldBe "sample:1"
            index.findById("missing") shouldBe null
        }
    })
