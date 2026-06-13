package com.jobsearch.common.web

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Maps uncaught exceptions to RFC 9457 Problem Details responses, so every service reports errors in
 * one standard shape. `@RestControllerAdvice` + [ProblemDetail] work identically on the servlet and
 * reactive stacks. Services add more specific `@ExceptionHandler`s on top of this baseline.
 */
@RestControllerAdvice
class ProblemDetailExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.message)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.message)

    private fun problem(
        status: HttpStatus,
        title: String,
        detail: String?,
    ): ProblemDetail =
        ProblemDetail.forStatus(status).apply {
            this.title = title
            detail?.let { this.detail = it }
        }
}
