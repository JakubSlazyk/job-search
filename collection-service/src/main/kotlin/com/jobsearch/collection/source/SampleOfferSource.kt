package com.jobsearch.collection.source

import com.google.protobuf.ByteString
import com.jobsearch.proto.collection.v1.RawOffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * A no-network demo source that always emits one hardcoded offer. Keeps the pipeline exercisable
 * without any external endpoint configured; the real adapters ([HimalayasOfferSource],
 * [FakeJobsScrapeOfferSource]) activate when their URLs are set.
 */
@Component
class SampleOfferSource : OfferSource {
    override val sourceName = "sample"

    override fun fetch(): Flux<RawOffer> = Flux.just(sample())

    fun sample(): RawOffer =
        RawOffer
            .newBuilder()
            .setSource(sourceName)
            .setExternalId("offer-1")
            .setFetchedAt(timestampOf())
            .setTitle("Senior Kotlin Engineer")
            .setCompany("ACME")
            .setUrl("https://example.com/offers/offer-1")
            .setLocation("Remote")
            .setDescription("We are hiring a senior Kotlin engineer.")
            .setContentType("application/json")
            .setRawContent(ByteString.copyFromUtf8("""{"id":"offer-1","title":"Senior Kotlin Engineer"}"""))
            .build()
}
