package com.jobsearch.tracker

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/** Start tracking an offer. An unknown status value fails JSON binding (400); offerId is required. */
data class TrackRequest(
    @field:NotBlank @field:Size(max = 255) val offerId: String,
    @field:NotNull val status: ApplicationStatus,
    @field:Size(max = 2000) val notes: String?,
)

/** Update the status/notes of an already-tracked offer. */
data class UpdateRequest(
    @field:NotNull val status: ApplicationStatus,
    @field:Size(max = 2000) val notes: String?,
)
