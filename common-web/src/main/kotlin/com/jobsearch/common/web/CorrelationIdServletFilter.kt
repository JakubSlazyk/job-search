package com.jobsearch.common.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Servlet-stack correlation filter: reuses an inbound [Correlation.HEADER] or mints a new id, puts
 * it in the SLF4J MDC for the duration of the request, and echoes it on the response.
 */
class CorrelationIdServletFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId =
            request.getHeader(Correlation.HEADER)?.takeIf(String::isNotBlank)
                ?: UUID.randomUUID().toString()
        MDC.put(Correlation.KEY, correlationId)
        response.setHeader(Correlation.HEADER, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(Correlation.KEY)
        }
    }
}
