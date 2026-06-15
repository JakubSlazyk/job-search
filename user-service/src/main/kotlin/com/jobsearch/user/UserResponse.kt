package com.jobsearch.user

/** Public shape of a user (profile + preferences) returned by the REST API. */
data class UserResponse(
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
    val preferences: PreferencesResponse,
) {
    companion object {
        fun from(view: UserView): UserResponse =
            with(view.user) {
                UserResponse(
                    subject = subject,
                    username = username,
                    email = email,
                    displayName = displayName,
                    fullName = fullName,
                    headline = headline,
                    phone = phone,
                    location = location,
                    linkedinUrl = linkedinUrl,
                    githubUrl = githubUrl,
                    websiteUrl = websiteUrl,
                    preferences =
                        PreferencesResponse(
                            view.preferences.emailNotificationsEnabled,
                            view.preferences.locale,
                        ),
                )
            }
    }
}

data class PreferencesResponse(
    val emailNotificationsEnabled: Boolean,
    val locale: String,
)
