package com.jobsearch.processing.domain

import com.google.protobuf.Timestamp
import com.jobsearch.common.domain.OfferIdentity
import com.jobsearch.proto.collection.v1.RawOffer
import com.jobsearch.proto.processing.v1.NormalizedOffer
import com.jobsearch.proto.processing.v1.Seniority
import java.time.Instant

/**
 * Source-agnostic canonicalization of a [RawOffer] into the unified [NormalizedOffer]: normalizes
 * whitespace, canonicalizes the location, and derives a [Seniority] from the title. Structural
 * extraction already happened at the edge (ADR 0005), so there is no per-source branching here.
 */
class OfferCanonicalizer {
    fun canonicalize(
        raw: RawOffer,
        normalizedAt: Instant = Instant.now(),
    ): NormalizedOffer {
        val title = canonicalizeText(raw.title)
        return NormalizedOffer
            .newBuilder()
            .setOfferId(OfferIdentity.offerId(raw.source, raw.externalId))
            .setSource(raw.source)
            .setExternalId(raw.externalId)
            .setTitle(title)
            .setCompany(canonicalizeText(raw.company))
            .setUrl(raw.url.trim())
            .setLocation(canonicalizeLocation(raw.location))
            .setDescription(canonicalizeText(raw.description))
            .setSeniority(seniorityOf(title))
            .setNormalizedAt(timestamp(normalizedAt))
            .build()
    }

    private fun canonicalizeText(value: String): String = value.trim().replace(WHITESPACE, " ")

    private fun canonicalizeLocation(value: String): String {
        val text = canonicalizeText(value)
        return if (text.contains("remote", ignoreCase = true)) "Remote" else text
    }

    private fun seniorityOf(title: String): Seniority {
        val normalized = title.lowercase()
        return when {
            LEAD_TERMS.any { it in normalized } -> Seniority.LEAD
            SENIOR_TERMS.any { it in normalized } -> Seniority.SENIOR
            JUNIOR_TERMS.any { it in normalized } -> Seniority.JUNIOR
            "mid" in normalized -> Seniority.MID
            else -> Seniority.SENIORITY_UNSPECIFIED
        }
    }

    private fun timestamp(instant: Instant): Timestamp =
        Timestamp
            .newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val LEAD_TERMS = listOf("lead", "principal", "staff")
        val SENIOR_TERMS = listOf("senior", "sr.")
        val JUNIOR_TERMS = listOf("junior", "jr.", "intern", "graduate")
    }
}
