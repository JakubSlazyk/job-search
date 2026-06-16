package com.jobsearch.notification

import java.time.Instant

/**
 * A user's match rule: [keyword] is matched case-insensitively as a substring of an offer's title or
 * company. Self-scoped by the Keycloak [subject].
 */
data class NotificationCriterion(
    val id: Long,
    val subject: String,
    val keyword: String,
    val createdAt: Instant,
)

/**
 * A new offer that matched one of a user's criteria — the unit pushed over email + WebSocket and
 * recorded as a [DeliveredNotification].
 */
data class OfferMatch(
    val offerId: String,
    val title: String,
    val company: String,
    val url: String,
)

/** A match that was delivered to a user, kept for the history endpoint and WebSocket catch-up. */
data class DeliveredNotification(
    val id: Long,
    val subject: String,
    val offerId: String,
    val title: String?,
    val company: String?,
    val url: String?,
    val deliveredAt: Instant,
)
