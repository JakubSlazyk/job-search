package com.jobsearch.processing.application

import com.jobsearch.proto.processing.v1.NormalizedOffer

/** Outbound port: the destination for canonicalized offers (implemented by an adapter). */
fun interface NormalizedOfferSink {
    fun send(offer: NormalizedOffer)
}
