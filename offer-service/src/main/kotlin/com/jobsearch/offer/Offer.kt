package com.jobsearch.offer

/** The canonical offer as stored and served by offer-service (§1.1 thin read model). */
data class Offer(
    val offerId: String,
    val source: String,
    val externalId: String,
    val title: String,
    val company: String,
    val url: String,
    val location: String,
    val description: String,
)
