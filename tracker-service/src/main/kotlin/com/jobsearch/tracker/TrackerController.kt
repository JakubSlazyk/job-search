package com.jobsearch.tracker

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Protected, self-scoped application-tracker endpoints. The §2.0 servlet resource-server chain
 * requires a valid Keycloak JWT (relayed as a Bearer token by the gateway BFF). Every operation
 * targets the caller's own records — the subject comes from the token, never a path variable.
 */
@RestController
@RequestMapping("/api/v1/tracker/applications")
class TrackerController(
    private val trackerService: TrackerService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
    ): List<ApplicationResponse> = trackerService.list(jwt.subjectOrThrow()).map(ApplicationResponse::from)

    @PostMapping
    fun track(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: TrackRequest,
    ): ApplicationResponse = ApplicationResponse.from(trackerService.track(jwt.subjectOrThrow(), request))

    @GetMapping("/{offerId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable offerId: String,
    ): ApplicationResponse = ApplicationResponse.from(trackerService.get(jwt.subjectOrThrow(), offerId))

    @PutMapping("/{offerId}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable offerId: String,
        @Valid @RequestBody request: UpdateRequest,
    ): ApplicationResponse = ApplicationResponse.from(trackerService.update(jwt.subjectOrThrow(), offerId, request))

    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun untrack(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable offerId: String,
    ) = trackerService.untrack(jwt.subjectOrThrow(), offerId)

    private fun Jwt.subjectOrThrow(): String = requireNotNull(subject) { "JWT has no subject claim" }
}
