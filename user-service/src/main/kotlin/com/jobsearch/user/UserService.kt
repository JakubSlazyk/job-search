package com.jobsearch.user

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

/**
 * Resolves the authenticated user from the relayed JWT, provisions them on first sight, and applies
 * self-scoped profile/preferences updates. [contactOf] is the read-only lookup the gRPC server
 * exposes to internal callers (notification-service, §2.4).
 */
@Service
class UserService(
    private val users: UserRepository,
) {
    /** The current user (profile + preferences), provisioning the records on first call. */
    fun getMe(jwt: Jwt): UserView = provision(jwt)

    fun updateProfile(
        jwt: Jwt,
        request: ProfileUpdateRequest,
    ): UserView {
        val view = provision(jwt)
        return view.copy(user = users.updateProfile(view.user.subject, request))
    }

    fun updatePreferences(
        jwt: Jwt,
        request: PreferencesUpdateRequest,
    ): UserView {
        val view = provision(jwt)
        return view.copy(preferences = users.updatePreferences(view.user.subject, request))
    }

    /** Read-only contact lookup by subject for internal gRPC callers; null when the user is unknown. */
    fun contactOf(subject: String): UserView? =
        users.findUser(subject)?.let { user ->
            users.findPreferences(subject)?.let { preferences -> UserView(user, preferences) }
        }

    private fun provision(jwt: Jwt): UserView {
        val subject = requireNotNull(jwt.subject) { "JWT has no subject claim" }
        val username = jwt.getClaimAsString(USERNAME_CLAIM) ?: subject
        val email = jwt.getClaimAsString(EMAIL_CLAIM)
        val user = users.upsertUser(subject, username, email)
        val preferences = users.ensurePreferences(subject)
        return UserView(user, preferences)
    }

    private companion object {
        const val USERNAME_CLAIM = "preferred_username"
        const val EMAIL_CLAIM = "email"
    }
}
