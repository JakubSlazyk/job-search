package com.jobsearch.user

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Protected user endpoints. The §2.0 servlet resource-server chain requires a valid Keycloak JWT
 * (relayed as a Bearer token by the gateway BFF) for everything under `/api/v1/users`.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    /** Returns the authenticated user, provisioning the record from the token on first call. */
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): UserResponse = UserResponse.from(userService.upsertFromToken(jwt))
}
