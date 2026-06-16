package com.jobsearch.notification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/** Add a match rule. The keyword is matched case-insensitively against an offer's title/company. */
data class CreateCriterionRequest(
    @field:NotBlank @field:Size(max = 255) val keyword: String,
)

/** Public shape of a saved match rule. */
data class CriterionResponse(
    val id: Long,
    val keyword: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(criterion: NotificationCriterion): CriterionResponse =
            CriterionResponse(criterion.id, criterion.keyword, criterion.createdAt)
    }
}

/** Public shape of a delivered notification (history + WebSocket payload). */
data class NotificationResponse(
    val offerId: String,
    val title: String?,
    val company: String?,
    val url: String?,
    val deliveredAt: Instant,
) {
    companion object {
        fun from(delivered: DeliveredNotification): NotificationResponse =
            NotificationResponse(
                offerId = delivered.offerId,
                title = delivered.title,
                company = delivered.company,
                url = delivered.url,
                deliveredAt = delivered.deliveredAt,
            )
    }
}

/** A criterion id was requested for deletion that the current user does not own. Mapped to 404. */
class CriterionNotFoundException(
    id: Long,
) : RuntimeException("No notification criterion '$id' for the current user")
