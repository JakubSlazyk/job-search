package com.jobsearch.collection

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Manual trigger: `POST /api/v1/collect` runs all configured sources once and reports how many
 * offers were published. A property-gated [ScheduledCollector] polls on an interval in production.
 */
@RestController
@RequestMapping("/api/v1/collect")
class CollectController(
    private val collector: OfferCollector,
) {
    @PostMapping
    fun collect(): Mono<ResponseEntity<Map<String, Int>>> =
        collector.collectAll().map { count -> ResponseEntity.accepted().body(mapOf("collected" to count)) }
}
