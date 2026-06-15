package com.jobsearch.offer

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/**
 * Postgres-backed canonical store (CQRS write model). Reads are served from the OpenSearch read
 * model, so this side only upserts — keyed on the natural `offer_id` via `INSERT ... ON CONFLICT`,
 * making re-consumption idempotent.
 */
@Repository
class OfferRepository(
    private val jdbc: JdbcClient,
) {
    fun upsert(offer: Offer) {
        jdbc
            .sql(
                """
                INSERT INTO offers
                    (offer_id, source, external_id, title, company, url, location, description, seniority, updated_at)
                VALUES
                    (:offerId, :source, :externalId, :title, :company, :url, :location, :description, :seniority, now())
                ON CONFLICT (offer_id) DO UPDATE SET
                    source = excluded.source,
                    external_id = excluded.external_id,
                    title = excluded.title,
                    company = excluded.company,
                    url = excluded.url,
                    location = excluded.location,
                    description = excluded.description,
                    seniority = excluded.seniority,
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
            .param("seniority", offer.seniority)
            .update()
    }
}
