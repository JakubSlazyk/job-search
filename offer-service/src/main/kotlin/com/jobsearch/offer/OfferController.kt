package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Browsing API over the OpenSearch read model (§1.4): optional full-text `query` plus exact-match
 * `source` / `location` / `seniority` filters, paginated. `GET /{offerId}` is a point lookup.
 * Documented via OpenAPI/springdoc (§1.6) — see [OpenApiConfig].
 */
@RestController
@RequestMapping("/api/v1/offers")
@Tag(name = "Offers", description = "Browse and search canonical job offers")
class OfferController(
    private val searchIndex: OfferSearchIndex,
) {
    @GetMapping
    @Operation(summary = "Search offers", description = "Optional full-text query with exact-match filters, paginated")
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) source: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) seniority: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<Offer> =
        searchIndex.search(
            OfferSearchCriteria.paged(query, source, location, seniority, page, size),
        )

    @GetMapping("/{offerId}")
    @Operation(summary = "Get an offer by id", description = "Point lookup by canonical offer id; 404 when absent")
    fun byId(
        @PathVariable offerId: String,
    ): ResponseEntity<Offer> =
        searchIndex.findById(offerId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
