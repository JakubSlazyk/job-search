package com.jobsearch.user

import java.time.Instant

/** A user record keyed on the Keycloak subject (JWT `sub`). */
data class User(
    val subject: String,
    val username: String,
    val email: String?,
    val createdAt: Instant,
    val lastSeenAt: Instant,
)
