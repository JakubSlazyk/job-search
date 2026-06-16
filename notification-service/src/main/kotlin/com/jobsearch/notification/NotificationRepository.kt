package com.jobsearch.notification

import io.r2dbc.spi.Readable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * Fully reactive Postgres store (R2DBC `DatabaseClient`). Criteria are self-scoped by `subject`;
 * matching is pushed into SQL (case-insensitive substring of title/company) so the consumer fans out
 * to matching subjects without loading every rule. The `processed_events` guard gives the
 * offer.published consumer event_id idempotency over at-least-once delivery (as in tracker-service).
 */
@Repository
class NotificationRepository(
    private val databaseClient: DatabaseClient,
) {
    /**
     * Record an event_id as processed, emitting true only the first time it is seen. The consumer gates
     * delivery on this so redelivered events (at-least-once) are no-ops.
     */
    fun markEventProcessed(eventId: String): Mono<Boolean> =
        databaseClient
            .sql("INSERT INTO processed_events (event_id) VALUES (:eventId) ON CONFLICT (event_id) DO NOTHING")
            .bind("eventId", eventId)
            .fetch()
            .rowsUpdated()
            .map { it > 0 }

    /** Distinct subjects whose keyword matches the offer's title or company (case-insensitive). */
    fun findMatchingSubjects(
        title: String,
        company: String,
    ): Flux<String> =
        databaseClient
            .sql(
                """
                SELECT DISTINCT subject FROM notification_criteria
                WHERE position(lower(keyword) IN lower(:title)) > 0
                   OR position(lower(keyword) IN lower(:company)) > 0
                """.trimIndent(),
            ).bind("title", title)
            .bind("company", company)
            .map { row -> row.get("subject", String::class.java)!! }
            .all()

    fun addCriterion(
        subject: String,
        keyword: String,
    ): Mono<NotificationCriterion> =
        databaseClient
            .sql(
                """
                INSERT INTO notification_criteria (subject, keyword) VALUES (:subject, :keyword)
                RETURNING id, subject, keyword, created_at
                """.trimIndent(),
            ).bind("subject", subject)
            .bind("keyword", keyword)
            .map(::mapCriterion)
            .one()

    fun listCriteria(subject: String): Flux<NotificationCriterion> =
        databaseClient
            .sql(
                """
                SELECT id, subject, keyword, created_at FROM notification_criteria
                WHERE subject = :subject ORDER BY created_at
                """.trimIndent(),
            ).bind("subject", subject)
            .map(::mapCriterion)
            .all()

    /** Delete one of the user's criteria; emits false when it does not exist for that subject. */
    fun deleteCriterion(
        subject: String,
        id: Long,
    ): Mono<Boolean> =
        databaseClient
            .sql("DELETE FROM notification_criteria WHERE subject = :subject AND id = :id")
            .bind("subject", subject)
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .map { it > 0 }

    fun recordDelivery(
        subject: String,
        match: OfferMatch,
    ): Mono<DeliveredNotification> =
        databaseClient
            .sql(
                """
                INSERT INTO delivered_notifications (subject, offer_id, title, company, url)
                VALUES (:subject, :offerId, :title, :company, :url)
                RETURNING id, subject, offer_id, title, company, url, delivered_at
                """.trimIndent(),
            ).bind("subject", subject)
            .bind("offerId", match.offerId)
            .bind("title", match.title)
            .bind("company", match.company)
            .bind("url", match.url)
            .map(::mapDelivery)
            .one()

    fun listDeliveries(subject: String): Flux<DeliveredNotification> =
        databaseClient
            .sql(
                """
                SELECT id, subject, offer_id, title, company, url, delivered_at
                FROM delivered_notifications WHERE subject = :subject ORDER BY delivered_at DESC
                """.trimIndent(),
            ).bind("subject", subject)
            .map(::mapDelivery)
            .all()

    private fun mapCriterion(row: Readable): NotificationCriterion =
        NotificationCriterion(
            id = row.get("id", Long::class.javaObjectType)!!,
            subject = row.get("subject", String::class.java)!!,
            keyword = row.get("keyword", String::class.java)!!,
            createdAt = row.get("created_at", OffsetDateTime::class.java)!!.toInstant(),
        )

    private fun mapDelivery(row: Readable): DeliveredNotification =
        DeliveredNotification(
            id = row.get("id", Long::class.javaObjectType)!!,
            subject = row.get("subject", String::class.java)!!,
            offerId = row.get("offer_id", String::class.java)!!,
            title = row.get("title", String::class.java),
            company = row.get("company", String::class.java),
            url = row.get("url", String::class.java),
            deliveredAt = row.get("delivered_at", OffsetDateTime::class.java)!!.toInstant(),
        )
}
