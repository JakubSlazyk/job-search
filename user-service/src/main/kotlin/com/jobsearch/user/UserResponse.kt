package com.jobsearch.user

/** Public shape of a user returned by the REST API. */
data class UserResponse(
    val subject: String,
    val username: String,
    val email: String?,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(user.subject, user.username, user.email)
    }
}
