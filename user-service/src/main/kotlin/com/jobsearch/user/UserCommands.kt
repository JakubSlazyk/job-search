package com.jobsearch.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

/** Editable profile fields. All optional; link fields are validated as URLs only when present. */
data class ProfileUpdateRequest(
    @field:Size(max = 255) val displayName: String?,
    @field:Size(max = 255) val fullName: String?,
    @field:Size(max = 255) val headline: String?,
    @field:Size(max = 64) val phone: String?,
    @field:Size(max = 255) val location: String?,
    @field:URL @field:Size(max = 512) val linkedinUrl: String?,
    @field:URL @field:Size(max = 512) val githubUrl: String?,
    @field:URL @field:Size(max = 512) val websiteUrl: String?,
)

/** Contact/channel preferences update. */
data class PreferencesUpdateRequest(
    val emailNotificationsEnabled: Boolean,
    @field:NotBlank @field:Size(max = 16) val locale: String,
)
