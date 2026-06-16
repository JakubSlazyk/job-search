package com.jobsearch.notification

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

/**
 * Maps request-validation failures to RFC 9457 `400`s and unknown criteria to `404`s. Ordered ahead of
 * common-web's baseline advice (whose catch-all would otherwise turn these into `500`s).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidation(ex: WebExchangeBindException): ProblemDetail {
        val detail = ex.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail)
    }

    @ExceptionHandler(CriterionNotFoundException::class)
    fun handleNotFound(ex: CriterionNotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Notification criterion not found", ex.message)

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
