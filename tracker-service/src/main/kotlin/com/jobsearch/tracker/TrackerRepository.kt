package com.jobsearch.tracker

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

/**
 * Postgres-backed store (Spring Data JDBC `JdbcClient`). Tracked offers are self-scoped by `subject`;
 * reads left-join the [OfferSnapshot] so a row renders even before its enrichment event arrives. The
 * snapshot upsert and the `processed_events` guard are written together in one transaction by
 * [EnrichmentService], giving event_id idempotency over at-least-once delivery.
 */
@Repository
class TrackerRepository(
    private val jdbc: JdbcClient,
) {
    /** Create or replace the user's tracking row for an offer (status + notes). */
    fun upsertApplication(
        subject: String,
        offerId: String,
        status: ApplicationStatus,
        notes: String?,
    ) {
        jdbc
            .sql(
                """
                INSERT INTO tracked_offers (subject, offer_id, status, notes)
                VALUES (:subject, :offerId, :status, :notes)
                ON CONFLICT (subject, offer_id) DO UPDATE SET
                    status = excluded.status,
                    notes = excluded.notes,
                    updated_at = now()
                """.trimIndent(),
            ).param("subject", subject)
            .param("offerId", offerId)
            .param("status", status.name)
            .param("notes", notes)
            .update()
    }

    /** Update an existing tracking row; returns false when the user is not tracking that offer. */
    fun updateApplication(
        subject: String,
        offerId: String,
        status: ApplicationStatus,
        notes: String?,
    ): Boolean =
        jdbc
            .sql(
                """
                UPDATE tracked_offers SET
                    status = :status,
                    notes = :notes,
                    updated_at = now()
                WHERE subject = :subject AND offer_id = :offerId
                """.trimIndent(),
            ).param("subject", subject)
            .param("offerId", offerId)
            .param("status", status.name)
            .param("notes", notes)
            .update() > 0

    /** Stop tracking an offer; returns false when there was nothing to remove. */
    fun deleteApplication(
        subject: String,
        offerId: String,
    ): Boolean =
        jdbc
            .sql("DELETE FROM tracked_offers WHERE subject = :subject AND offer_id = :offerId")
            .param("subject", subject)
            .param("offerId", offerId)
            .update() > 0

    fun findApplication(
        subject: String,
        offerId: String,
    ): TrackedOfferView? =
        jdbc
            .sql("$VIEW_SELECT WHERE t.subject = :subject AND t.offer_id = :offerId")
            .param("subject", subject)
            .param("offerId", offerId)
            .query(::mapView)
            .optional()
            .orElse(null)

    fun listApplications(subject: String): List<TrackedOfferView> =
        jdbc
            .sql("$VIEW_SELECT WHERE t.subject = :subject ORDER BY t.created_at DESC")
            .param("subject", subject)
            .query(::mapView)
            .list()

    /**
     * Record an event_id as processed, returning true only the first time it is seen. Callers gate the
     * snapshot upsert on this so redelivered events (at-least-once) are no-ops.
     */
    fun markEventProcessed(eventId: String): Boolean =
        jdbc
            .sql("INSERT INTO processed_events (event_id) VALUES (:eventId) ON CONFLICT (event_id) DO NOTHING")
            .param("eventId", eventId)
            .update() > 0

    /** Upsert the local display copy of an offer from an `offer.published` event. */
    fun upsertSnapshot(
        offerId: String,
        title: String?,
        company: String?,
        url: String?,
        publishedAt: Instant?,
    ) {
        jdbc
            .sql(
                """
                INSERT INTO offer_snapshots (offer_id, title, company, url, published_at, updated_at)
                VALUES (:offerId, :title, :company, :url, :publishedAt, now())
                ON CONFLICT (offer_id) DO UPDATE SET
                    title = excluded.title,
                    company = excluded.company,
                    url = excluded.url,
                    published_at = excluded.published_at,
                    updated_at = now()
                """.trimIndent(),
            ).param("offerId", offerId)
            .param("title", title)
            .param("company", company)
            .param("url", url)
            .param("publishedAt", publishedAt?.let(Timestamp::from))
            .update()
    }

    private fun mapView(
        rs: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNum: Int,
    ): TrackedOfferView {
        val tracked =
            TrackedOffer(
                subject = rs.getString("subject"),
                offerId = rs.getString("offer_id"),
                status = ApplicationStatus.valueOf(rs.getString("status")),
                notes = rs.getString("notes"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
        // LEFT JOIN: snap_offer_id is null exactly when no enrichment event has been seen yet.
        val snapshot =
            rs.getString("snap_offer_id")?.let { snapOfferId ->
                OfferSnapshot(
                    offerId = snapOfferId,
                    title = rs.getString("title"),
                    company = rs.getString("company"),
                    url = rs.getString("url"),
                    publishedAt = rs.getTimestamp("published_at")?.toInstant(),
                )
            }
        return TrackedOfferView(tracked, snapshot)
    }

    private companion object {
        val VIEW_SELECT =
            """
            SELECT t.subject, t.offer_id, t.status, t.notes, t.created_at, t.updated_at,
                   s.offer_id AS snap_offer_id, s.title, s.company, s.url, s.published_at
            FROM tracked_offers t
            LEFT JOIN offer_snapshots s ON s.offer_id = t.offer_id
            """.trimIndent()
    }
}
