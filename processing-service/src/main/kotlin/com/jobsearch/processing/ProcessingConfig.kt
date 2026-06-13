package com.jobsearch.processing

import com.jobsearch.processing.application.NormalizedOfferSink
import com.jobsearch.processing.application.OfferProcessor
import com.jobsearch.processing.domain.OfferCanonicalizer
import com.jobsearch.processing.domain.OfferDeduplicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Composition root: wires the framework-free domain + application beans to the Kafka adapters. */
@Configuration
class ProcessingConfig {
    @Bean
    fun offerCanonicalizer(): OfferCanonicalizer = OfferCanonicalizer()

    @Bean
    fun offerDeduplicator(): OfferDeduplicator = OfferDeduplicator()

    @Bean
    fun offerProcessor(
        canonicalizer: OfferCanonicalizer,
        deduplicator: OfferDeduplicator,
        sink: NormalizedOfferSink,
    ): OfferProcessor = OfferProcessor(canonicalizer, deduplicator, sink)
}
