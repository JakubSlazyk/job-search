package com.jobsearch.offer

/** The canonical offer as stored (Postgres write model) and indexed/served (OpenSearch read model). */
data class Offer(
    val offerId: String,
    val source: String,
    val externalId: String,
    val title: String,
    val company: String,
    val url: String,
    val location: String,
    val description: String,
    val seniority: String,
)
