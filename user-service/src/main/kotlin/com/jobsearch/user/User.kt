package com.jobsearch.user

import java.time.Instant

/** A user record keyed on the Keycloak subject (JWT `sub`), with editable profile fields. */
data class User(
    val subject: String,
    val username: String,
    val email: String?,
    val displayName: String?,
    val fullName: String?,
    val headline: String?,
    val phone: String?,
    val location: String?,
    val linkedinUrl: String?,
    val githubUrl: String?,
    val websiteUrl: String?,
    val createdAt: Instant,
    val lastSeenAt: Instant,
)

/** Contact/channel preferences (1:1 with [User]). Matching criteria live in notification-service. */
data class UserPreferences(
    val subject: String,
    val emailNotificationsEnabled: Boolean,
    val locale: String,
)

/** A user together with their preferences — the aggregate the service hands to the API/gRPC layer. */
data class UserView(
    val user: User,
    val preferences: UserPreferences,
)
