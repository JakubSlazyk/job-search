package com.jobsearch.offer

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/**
 * Postgres-backed store for canonical offers (write side). Upsert is keyed on the natural `offer_id`
 * via `INSERT ... ON CONFLICT`, so re-consuming the same offer is idempotent.
 */
@Repository
class OfferRepository(
    private val jdbc: JdbcClient,
) {
    fun upsert(offer: Offer) {
        jdbc
            .sql(
                """
                INSERT INTO offers (offer_id, source, external_id, title, company, url, location, description, updated_at)
                VALUES (:offerId, :source, :externalId, :title, :company, :url, :location, :description, now())
                ON CONFLICT (offer_id) DO UPDATE SET
                    source = excluded.source,
                    external_id = excluded.external_id,
                    title = excluded.title,
                    company = excluded.company,
                    url = excluded.url,
                    location = excluded.location,
                    description = excluded.description,
                    updated_at = now()
                """.trimIndent(),
            ).param("offerId", offer.offerId)
            .param("source", offer.source)
            .param("externalId", offer.externalId)
            .param("title", offer.title)
            .param("company", offer.company)
            .param("url", offer.url)
            .param("location", offer.location)
            .param("description", offer.description)
            .update()
    }

    fun findAll(): List<Offer> = jdbc.sql("$SELECT_COLUMNS ORDER BY offer_id").query(rowMapper).list()

    fun findById(offerId: String): Offer? =
        jdbc
            .sql("$SELECT_COLUMNS WHERE offer_id = :offerId")
            .param("offerId", offerId)
            .query(rowMapper)
            .optional()
            .orElse(null)

    private companion object {
        const val SELECT_COLUMNS =
            "SELECT offer_id, source, external_id, title, company, url, location, description FROM offers"

        val rowMapper =
            RowMapper { rs, _ ->
                Offer(
                    offerId = rs.getString("offer_id"),
                    source = rs.getString("source"),
                    externalId = rs.getString("external_id"),
                    title = rs.getString("title"),
                    company = rs.getString("company"),
                    url = rs.getString("url"),
                    location = rs.getString("location"),
                    description = rs.getString("description"),
                )
            }
    }
}
