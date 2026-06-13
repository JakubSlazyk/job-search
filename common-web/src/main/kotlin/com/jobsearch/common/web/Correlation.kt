package com.jobsearch.common.web

/**
 * Shared constants for request correlation. The id is read from (or generated for) every inbound
 * request, echoed on the response, and placed where logs can pick it up so a single request can be
 * traced across services.
 */
object Correlation {
    /** Request/response header carrying the correlation id. */
    const val HEADER = "X-Correlation-Id"

    /** SLF4J MDC key (servlet stack) and Reactor context key (reactive stack). */
    const val KEY = "correlationId"
}
