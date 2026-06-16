package com.jobsearch.notification

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Protected, self-scoped notification endpoints (reactive). The §2.0 reactive resource-server chain
 * requires a valid Keycloak JWT relayed by the gateway BFF. Every operation targets the caller's own
 * records — the subject comes from the token, never the client.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val repository: NotificationRepository,
) {
    @GetMapping("/criteria")
    fun listCriteria(
        @AuthenticationPrincipal jwt: Jwt,
    ): Flux<CriterionResponse> = repository.listCriteria(jwt.subjectOrThrow()).map(CriterionResponse::from)

    @PostMapping("/criteria")
    fun addCriterion(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateCriterionRequest,
    ): Mono<CriterionResponse> =
        repository.addCriterion(jwt.subjectOrThrow(), request.keyword.trim()).map(CriterionResponse::from)

    @DeleteMapping("/criteria/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCriterion(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): Mono<Void> =
        repository.deleteCriterion(jwt.subjectOrThrow(), id).flatMap { removed ->
            if (removed) Mono.empty() else Mono.error(CriterionNotFoundException(id))
        }

    @GetMapping
    fun history(
        @AuthenticationPrincipal jwt: Jwt,
    ): Flux<NotificationResponse> = repository.listDeliveries(jwt.subjectOrThrow()).map(NotificationResponse::from)

    private fun Jwt.subjectOrThrow(): String = requireNotNull(subject) { "JWT has no subject claim" }
}
