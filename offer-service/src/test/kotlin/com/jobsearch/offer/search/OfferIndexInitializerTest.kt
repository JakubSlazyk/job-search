package com.jobsearch.offer.search

import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient

class OfferIndexInitializerTest :
    StringSpec({
        "creates the index with a mapping when it is absent" {
            val indices = mockk<OpenSearchIndicesClient>(relaxed = true)
            every { indices.exists(any<ExistsRequest>()) } returns mockk { every { value() } returns false }
            val client = mockk<OpenSearchClient> { every { indices() } returns indices }

            OfferIndexInitializer(client).ensureIndex()

            verify(exactly = 1) { indices.create(any<CreateIndexRequest>()) }
        }

        "does nothing when the index already exists" {
            val indices = mockk<OpenSearchIndicesClient>(relaxed = true)
            every { indices.exists(any<ExistsRequest>()) } returns mockk { every { value() } returns true }
            val client = mockk<OpenSearchClient> { every { indices() } returns indices }

            OfferIndexInitializer(client).ensureIndex()

            verify(exactly = 0) { indices.create(any<CreateIndexRequest>()) }
        }
    })
