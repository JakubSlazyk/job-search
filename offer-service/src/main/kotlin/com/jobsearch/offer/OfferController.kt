package com.jobsearch.offer

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Thin browsing API over the canonical store (§1.1). Search/filter via OpenSearch arrives in §1.4. */
@RestController
@RequestMapping("/api/v1/offers")
class OfferController(
    private val repository: OfferRepository,
) {
    @GetMapping
    fun list(): List<Offer> = repository.findAll()

    @GetMapping("/{offerId}")
    fun byId(
        @PathVariable offerId: String,
    ): ResponseEntity<Offer> =
        repository
            .findById(offerId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
}
