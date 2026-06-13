package com.jobsearch.common.web

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CorrelationIdServletFilterTest :
    StringSpec({
        val filter = CorrelationIdServletFilter()

        "reuses an inbound correlation id, exposes it via MDC, and echoes it on the response" {
            val request = MockHttpServletRequest().apply { addHeader(Correlation.HEADER, "abc-123") }
            val response = MockHttpServletResponse()
            var seenInChain: String? = null

            filter.doFilter(request, response) { _, _ -> seenInChain = MDC.get(Correlation.KEY) }

            seenInChain shouldBe "abc-123"
            response.getHeader(Correlation.HEADER) shouldBe "abc-123"
            MDC.get(Correlation.KEY).shouldBeNull() // cleared after the request
        }

        "generates a correlation id when none is supplied" {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            filter.doFilter(request, response) { _, _ -> }

            response.getHeader(Correlation.HEADER)!!.shouldNotBeEmpty()
        }
    })
