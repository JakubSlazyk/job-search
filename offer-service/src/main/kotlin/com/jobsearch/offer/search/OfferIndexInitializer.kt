package com.jobsearch.offer.search

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.Property
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Creates the `offers` index with an explicit mapping once the app is up (idempotent): title /
 * company / description are full-text (`text`); source / location / seniority / ids are exact-match
 * (`keyword`) for filtering.
 */
@Component
class OfferIndexInitializer(
    private val client: OpenSearchClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndex() {
        if (client.indices().exists(ExistsRequest.Builder().index(OFFERS_INDEX).build()).value()) return

        val mapping =
            TypeMapping
                .Builder()
                .properties("offerId", keyword())
                .properties("source", keyword())
                .properties("externalId", keyword())
                .properties("title", text())
                .properties("company", text())
                .properties("url", keyword())
                .properties("location", keyword())
                .properties("description", text())
                .properties("seniority", keyword())
                .build()

        client.indices().create(
            CreateIndexRequest
                .Builder()
                .index(OFFERS_INDEX)
                .mappings(mapping)
                .build(),
        )
        log.info("Created OpenSearch index '{}'", OFFERS_INDEX)
    }

    private fun keyword(): Property = Property.of { p -> p.keyword { k -> k } }

    private fun text(): Property = Property.of { p -> p.text { t -> t } }
}
