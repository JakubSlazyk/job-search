package com.jobsearch.user

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

/**
 * Resolves the authenticated user from the relayed JWT and provisions them on first sight.
 *
 * The Keycloak `sub` claim is the stable identity; `preferred_username` and `email` are best-effort
 * profile bits (username falls back to the subject if Keycloak omits it).
 */
@Service
class UserService(
    private val users: UserRepository,
) {
    fun upsertFromToken(jwt: Jwt): User {
        val subject = requireNotNull(jwt.subject) { "JWT has no subject claim" }
        val username = jwt.getClaimAsString(USERNAME_CLAIM) ?: subject
        val email = jwt.getClaimAsString(EMAIL_CLAIM)
        return users.upsert(subject, username, email)
    }

    private companion object {
        const val USERNAME_CLAIM = "preferred_username"
        const val EMAIL_CLAIM = "email"
    }
}
