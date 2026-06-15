package com.jobsearch.offer.grpc

import com.jobsearch.offer.Offer
import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import com.jobsearch.proto.offer.v1.GetOfferRequest
import com.jobsearch.proto.offer.v1.GetOfferResponse
import com.jobsearch.proto.offer.v1.SearchOffersRequest
import com.jobsearch.proto.offer.v1.SearchOffersResponse
import io.grpc.stub.StreamObserver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

private val sample =
    Offer(
        offerId = "sample:1",
        source = "sample",
        externalId = "1",
        title = "Engineer",
        company = "ACME",
        url = "https://example.com/1",
        location = "Remote",
        description = "desc",
        seniority = "SENIOR",
    )

/** Captures the single unary response emitted to a [StreamObserver]. */
private class RecordingObserver<T> : StreamObserver<T> {
    val values = mutableListOf<T>()
    var completed = false

    override fun onNext(value: T) {
        values += value
    }

    override fun onError(t: Throwable) = throw t

    override fun onCompleted() {
        completed = true
    }

    fun single(): T {
        check(completed) { "stream not completed" }
        return values.single()
    }
}

/**
 * Unit-tests the gRPC `offer.v1` service against its generated base (real proto messages, read model
 * mocked) by invoking the unary methods directly with a recording [StreamObserver] — the no-context
 * style of the REST/search unit tests, without standing up a transport.
 */
class OfferGrpcServiceTest :
    StringSpec({
        "GetOffer returns the offer when found" {
            val index = mockk<OfferSearchIndex> { every { findById("sample:1") } returns sample }
            val observer = RecordingObserver<GetOfferResponse>()

            OfferGrpcService(index).getOffer(GetOfferRequest.newBuilder().setOfferId("sample:1").build(), observer)

            val response = observer.single()
            response.hasOffer() shouldBe true
            response.offer.offerId shouldBe "sample:1"
            response.offer.title shouldBe "Engineer"
            response.offer.seniority shouldBe "SENIOR"
        }

        "GetOffer leaves the offer unset when missing" {
            val index = mockk<OfferSearchIndex> { every { findById("missing") } returns null }
            val observer = RecordingObserver<GetOfferResponse>()

            OfferGrpcService(index).getOffer(GetOfferRequest.newBuilder().setOfferId("missing").build(), observer)

            observer.single().hasOffer() shouldBe false
        }

        "SearchOffers maps blank filters to null and applies default size/paging" {
            val criteria = slot<OfferSearchCriteria>()
            val index = mockk<OfferSearchIndex> { every { search(capture(criteria)) } returns listOf(sample) }
            val observer = RecordingObserver<SearchOffersResponse>()

            OfferGrpcService(index).searchOffers(
                SearchOffersRequest
                    .newBuilder()
                    .setQuery("kotlin")
                    .setSeniority("SENIOR")
                    .setPage(2)
                    .build(),
                observer,
            )

            observer.single().offersList.map { it.offerId } shouldContainExactly listOf("sample:1")
            criteria.captured.query shouldBe "kotlin"
            criteria.captured.seniority shouldBe "SENIOR"
            criteria.captured.source shouldBe null // blank proto string -> no filter
            criteria.captured.size shouldBe 20 // size <= 0 -> default
            criteria.captured.from shouldBe 40 // page 2 * default size 20
        }
    })
