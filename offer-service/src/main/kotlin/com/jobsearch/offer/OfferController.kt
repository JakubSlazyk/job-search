package com.jobsearch.offer

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Browsing API over the OpenSearch read model (§1.4): optional full-text `query` plus exact-match
 * `source` / `location` / `seniority` filters, paginated. `GET /{offerId}` is a point lookup.
 */
@RestController
@RequestMapping("/api/v1/offers")
class OfferController(
    private val searchIndex: OfferSearchIndex,
) {
    @GetMapping
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) source: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) seniority: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
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

    @GetMapping("/{offerId}")
    fun byId(
        @PathVariable offerId: String,
    ): ResponseEntity<Offer> =
        searchIndex.findById(offerId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
