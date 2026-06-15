package com.jobsearch.collection

import com.jobsearch.collection.source.OfferSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Runs every configured [OfferSource] and publishes the fetched offers to `raw-offers`. A failure in
 * one source is isolated (it contributes nothing) so the others still run. Returns the number of
 * offers published.
 */
@Service
class OfferCollector(
    private val sources: List<OfferSource>,
    private val publisher: RawOfferPublisher,
) {
    fun collectAll(): Mono<Int> =
        Flux
            .fromIterable(sources)
            .flatMap { source -> source.fetch().onErrorResume { Flux.empty() } }
            .doOnNext(publisher::publish)
            .count()
            .map(Long::toInt)
}
