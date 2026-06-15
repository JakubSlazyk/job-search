package com.jobsearch.offer.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe

class OpenSearchConfigTest :
    StringSpec({
        "builds a transport and client (no connection happens until first request)" {
            val config = OpenSearchConfig()
            val transport = config.openSearchTransport(OpenSearchProperties("http", "localhost", 9200))
            try {
                config.openSearchClient(transport) shouldNotBe null
            } finally {
                transport.close()
            }
        }
    })
