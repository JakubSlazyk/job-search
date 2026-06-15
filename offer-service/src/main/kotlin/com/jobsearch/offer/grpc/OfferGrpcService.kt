package com.jobsearch.offer.grpc

import com.jobsearch.offer.search.OfferSearchCriteria
import com.jobsearch.offer.search.OfferSearchIndex
import com.jobsearch.proto.offer.v1.GetOfferRequest
import com.jobsearch.proto.offer.v1.GetOfferResponse
import com.jobsearch.proto.offer.v1.OfferQueryServiceGrpc
import com.jobsearch.proto.offer.v1.SearchOffersRequest
import com.jobsearch.proto.offer.v1.SearchOffersResponse
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import com.jobsearch.offer.Offer as DomainOffer
import com.jobsearch.proto.offer.v1.Offer as ProtoOffer

/**
 * Internal gRPC read API (§1.6) over the OpenSearch read model — the `offer.v1` contract lives in
 * common-domain (ADR 0003). Mirrors the REST/GraphQL surface for callers arriving in Phase 3
 * (chatbot, CV tailoring). Spring Boot's gRPC server starter registers this `BindableService` bean
 * and serves it on the Netty gRPC port (default :9090).
 */
@Service
class OfferGrpcService(
    private val searchIndex: OfferSearchIndex,
) : OfferQueryServiceGrpc.OfferQueryServiceImplBase() {
    override fun getOffer(
        request: GetOfferRequest,
        responseObserver: StreamObserver<GetOfferResponse>,
    ) {
        val response = GetOfferResponse.newBuilder()
        searchIndex.findById(request.offerId)?.let { response.offer = it.toProto() }
        responseObserver.onNext(response.build())
        responseObserver.onCompleted()
    }

    override fun searchOffers(
        request: SearchOffersRequest,
        responseObserver: StreamObserver<SearchOffersResponse>,
    ) {
        val hits =
            searchIndex.search(
                OfferSearchCriteria.paged(
                    query = request.query,
                    source = request.source.ifBlank { null },
                    location = request.location.ifBlank { null },
                    seniority = request.seniority.ifBlank { null },
                    page = request.page,
                    size = request.size,
                ),
            )
        responseObserver.onNext(
            SearchOffersResponse.newBuilder().addAllOffers(hits.map { it.toProto() }).build(),
        )
        responseObserver.onCompleted()
    }
}

private fun DomainOffer.toProto(): ProtoOffer =
    ProtoOffer
        .newBuilder()
        .setOfferId(offerId)
        .setSource(source)
        .setExternalId(externalId)
        .setTitle(title)
        .setCompany(company)
        .setUrl(url)
        .setLocation(location)
        .setDescription(description)
        .setSeniority(seniority)
        .build()
