package com.jobsearch.collection

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Per-source endpoints. Blank means the source is inactive (its adapter no-ops), so sources are
 * opt-in via configuration without code changes.
 */
@ConfigurationProperties(prefix = "collection.sources")
data class CollectionSourceProperties(
    val httpJsonUrl: String = "",
    val htmlScrapeUrl: String = "",
)
