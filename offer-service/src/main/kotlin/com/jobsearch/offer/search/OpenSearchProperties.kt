package com.jobsearch.offer.search

import org.springframework.boot.context.properties.ConfigurationProperties

/** Connection settings for the OpenSearch read model. */
@ConfigurationProperties(prefix = "opensearch")
data class OpenSearchProperties(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 9200,
)
