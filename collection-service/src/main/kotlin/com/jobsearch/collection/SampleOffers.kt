package com.jobsearch.collection

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.jobsearch.proto.collection.v1.RawOffer
import java.time.Instant

/**
 * Builds the single hardcoded RawOffer the walking skeleton (§1.1) pushes through the pipeline.
 * Real per-source adapters replace this in §1.2 (ADR 0005).
 */
object SampleOffers {
    fun sample(fetchedAt: Instant = Instant.now()): RawOffer =
        RawOffer
            .newBuilder()
            .setSource("sample")
            .setExternalId("offer-1")
            .setFetchedAt(
                Timestamp
                    .newBuilder()
                    .setSeconds(fetchedAt.epochSecond)
                    .setNanos(fetchedAt.nano)
                    .build(),
            ).setTitle("Senior Kotlin Engineer")
            .setCompany("ACME")
            .setUrl("https://example.com/offers/offer-1")
            .setLocation("Remote")
            .setDescription("We are hiring a senior Kotlin engineer.")
            .setContentType("application/json")
            .setRawContent(ByteString.copyFromUtf8("""{"id":"offer-1","title":"Senior Kotlin Engineer"}"""))
            .build()
}
