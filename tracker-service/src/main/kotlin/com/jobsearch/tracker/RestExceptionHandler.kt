package com.jobsearch.tracker

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Maps request-validation failures to RFC 9457 `400`s and missing tracked offers to `404`s. Ordered
 * ahead of common-web's baseline advice (whose catch-all `Exception` handler would otherwise turn
 * these into `500`s) so the specific handlers win.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val detail = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Malformed request body", ex.mostSpecificCause.message)

    @ExceptionHandler(TrackedOfferNotFoundException::class)
    fun handleNotFound(ex: TrackedOfferNotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Tracked offer not found", ex.message)

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
