package com.jobsearch.collection

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Manual trigger for the walking skeleton: `POST /api/v1/collect` emits the sample offer so the
 * pipeline can be exercised end-to-end with curl. A scheduled, multi-source collector replaces this
 * in §1.2.
 */
@RestController
@RequestMapping("/api/v1/collect")
class CollectController(
    private val publisher: RawOfferPublisher,
) {
    @PostMapping
    fun collectSample(): ResponseEntity<Map<String, String>> {
        val offerId = publisher.publish(SampleOffers.sample())
        return ResponseEntity.accepted().body(mapOf("offerId" to offerId))
    }
}
