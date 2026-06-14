package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/**
 * GraphQL query side (§1.6) over the OpenSearch read model — the flexible-querying surface alongside
 * REST ([OfferController]) and gRPC. Schema in `resources/graphql/schema.graphqls`; pagination
 * defaults (`page`/`size`) come from the schema, so the arguments are always populated.
 */
@Controller
class OfferGraphQlController(
    private val searchIndex: OfferSearchIndex,
) {
    @QueryMapping
    fun offers(
        @Argument query: String?,
        @Argument source: String?,
        @Argument location: String?,
        @Argument seniority: String?,
        @Argument page: Int,
        @Argument size: Int,
    ): List<Offer> =
        searchIndex.search(
            OfferSearchCriteria(
                query = query.orEmpty(),
                source = source,
                location = location,
                seniority = seniority,
                from = page * size,
                size = size,
            ),
        )

    @QueryMapping
    fun offer(
        @Argument offerId: String,
    ): Offer? = searchIndex.findById(offerId)
}
