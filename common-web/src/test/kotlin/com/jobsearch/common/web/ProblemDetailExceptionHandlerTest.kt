package com.jobsearch.common.web

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus

class ProblemDetailExceptionHandlerTest :
    StringSpec({
        val handler = ProblemDetailExceptionHandler()

        "maps IllegalArgumentException to a 400 problem detail" {
            val problem = handler.handleIllegalArgument(IllegalArgumentException("bad input"))

            problem.status shouldBe HttpStatus.BAD_REQUEST.value()
            problem.title shouldBe "Invalid request"
            problem.detail shouldBe "bad input"
        }

        "maps unexpected exceptions to a 500 problem detail" {
            val problem = handler.handleUnexpected(RuntimeException("boom"))

            problem.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
            problem.title shouldBe "Internal server error"
            problem.detail shouldBe "boom"
        }
    })
