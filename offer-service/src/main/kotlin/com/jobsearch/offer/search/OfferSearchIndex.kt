package com.jobsearch.offer.search

import com.jobsearch.offer.Offer
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.GetRequest
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.stereotype.Component

internal const val OFFERS_INDEX = "offers"

/** Mutable, Jackson-friendly read-model document deserialized from OpenSearch hits. */
class OfferDocument {
    var offerId: String = ""
    var source: String = ""
    var externalId: String = ""
    var title: String = ""
    var company: String = ""
    var url: String = ""
    var location: String = ""
    var description: String = ""
    var seniority: String = ""

    fun toOffer(): Offer = Offer(offerId, source, externalId, title, company, url, location, description, seniority)
}

/** Selection + filtering criteria for browsing the read model. */
data class OfferSearchCriteria(
    val query: String = "",
    val source: String? = null,
    val location: String? = null,
    val seniority: String? = null,
    val from: Int = 0,
    val size: Int = 20,
)

/**
 * OpenSearch read model (CQRS query side): indexes offers on upsert and serves browse / full-text
 * search / filtering. Full-text runs over title/company/description; source/location/seniority are
 * exact-match keyword filters.
 */
@Component
class OfferSearchIndex(
    private val client: OpenSearchClient,
) {
    fun index(offer: Offer) {
        client.index(
            IndexRequest
                .Builder<Offer>()
                .index(OFFERS_INDEX)
                .id(offer.offerId)
                .document(offer)
                .build(),
        )
    }

    fun findById(offerId: String): Offer? {
        val request =
            GetRequest
                .Builder()
                .index(OFFERS_INDEX)
                .id(offerId)
                .build()
        return client.get(request, OfferDocument::class.java).source()?.toOffer()
    }

    fun search(criteria: OfferSearchCriteria): List<Offer> {
        val request =
            SearchRequest
                .Builder()
                .index(OFFERS_INDEX)
                .from(criteria.from)
                .size(criteria.size)
                .query(buildQuery(criteria))
                .build()
        return client
            .search(request, OfferDocument::class.java)
            .hits()
            .hits()
            .mapNotNull { it.source()?.toOffer() }
    }

    internal fun buildQuery(criteria: OfferSearchCriteria): Query {
        val bool = BoolQuery.Builder()
        if (criteria.query.isBlank()) {
            bool.must(Query.of { q -> q.matchAll { it } })
        } else {
            bool.must(
                Query.of { q ->
                    q.multiMatch { m -> m.fields("title", "company", "description").query(criteria.query) }
                },
            )
        }
        criteria.source?.let { bool.filter(termQuery("source", it)) }
        criteria.location?.let { bool.filter(termQuery("location", it)) }
        criteria.seniority?.let { bool.filter(termQuery("seniority", it)) }
        return Query.of { q -> q.bool(bool.build()) }
    }

    private fun termQuery(
        field: String,
        value: String,
    ): Query = Query.of { q -> q.term { t -> t.field(field).value(FieldValue.of(value)) } }
}
