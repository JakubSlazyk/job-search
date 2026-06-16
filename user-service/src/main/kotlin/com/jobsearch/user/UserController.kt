package com.jobsearch.user

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Protected, self-scoped user endpoints. The §2.0 servlet resource-server chain requires a valid
 * Keycloak JWT (relayed as a Bearer token by the gateway BFF). Every operation targets the caller's
 * own record — the subject comes from the token, never a path variable.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    /** The current user (profile + preferences), provisioning the record on first call. */
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): UserResponse = UserResponse.from(userService.getMe(jwt))

    @PutMapping("/me/profile")
    fun updateProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ProfileUpdateRequest,
    ): UserResponse = UserResponse.from(userService.updateProfile(jwt, request))

    @PutMapping("/me/preferences")
    fun updatePreferences(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: PreferencesUpdateRequest,
    ): UserResponse = UserResponse.from(userService.updatePreferences(jwt, request))
}
