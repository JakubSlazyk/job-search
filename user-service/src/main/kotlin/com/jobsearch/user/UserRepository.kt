package com.jobsearch.user

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Postgres-backed user + preferences store (Spring Data JDBC `JdbcClient`). The login upsert is
 * keyed on the natural `subject` and only touches identity columns, so user-edited profile fields
 * are never clobbered. Preferences are a 1:1 table provisioned with defaults on first login.
 */
@Repository
class UserRepository(
    private val jdbc: JdbcClient,
) {
    /** Provision/refresh the user from token claims (profile columns untouched). */
    fun upsertUser(
        subject: String,
        username: String,
        email: String?,
    ): User =
        jdbc
            .sql(
                """
                INSERT INTO users (subject, username, email, last_seen_at)
                VALUES (:subject, :username, :email, now())
                ON CONFLICT (subject) DO UPDATE SET
                    username = excluded.username,
                    email = excluded.email,
                    last_seen_at = now()
                RETURNING $USER_COLUMNS
                """.trimIndent(),
            ).param("subject", subject)
            .param("username", username)
            .param("email", email)
            .query(::mapUser)
            .single()

    /** Ensure a default preferences row exists and return it (no-op update so RETURNING yields it). */
    fun ensurePreferences(subject: String): UserPreferences =
        jdbc
            .sql(
                """
                INSERT INTO user_preferences (subject)
                VALUES (:subject)
                ON CONFLICT (subject) DO UPDATE SET locale = user_preferences.locale
                RETURNING $PREFERENCES_COLUMNS
                """.trimIndent(),
            ).param("subject", subject)
            .query(::mapPreferences)
            .single()

    fun findUser(subject: String): User? =
        jdbc
            .sql("SELECT $USER_COLUMNS FROM users WHERE subject = :subject")
            .param("subject", subject)
            .query(::mapUser)
            .optional()
            .orElse(null)

    fun findPreferences(subject: String): UserPreferences? =
        jdbc
            .sql("SELECT $PREFERENCES_COLUMNS FROM user_preferences WHERE subject = :subject")
            .param("subject", subject)
            .query(::mapPreferences)
            .optional()
            .orElse(null)

    fun updateProfile(
        subject: String,
        request: ProfileUpdateRequest,
    ): User =
        jdbc
            .sql(
                """
                UPDATE users SET
                    display_name = :displayName,
                    full_name = :fullName,
                    headline = :headline,
                    phone = :phone,
                    location = :location,
                    linkedin_url = :linkedinUrl,
                    github_url = :githubUrl,
                    website_url = :websiteUrl
                WHERE subject = :subject
                RETURNING $USER_COLUMNS
                """.trimIndent(),
            ).param("subject", subject)
            .param("displayName", request.displayName)
            .param("fullName", request.fullName)
            .param("headline", request.headline)
            .param("phone", request.phone)
            .param("location", request.location)
            .param("linkedinUrl", request.linkedinUrl)
            .param("githubUrl", request.githubUrl)
            .param("websiteUrl", request.websiteUrl)
            .query(::mapUser)
            .single()

    fun updatePreferences(
        subject: String,
        request: PreferencesUpdateRequest,
    ): UserPreferences =
        jdbc
            .sql(
                """
                UPDATE user_preferences SET
                    email_notifications_enabled = :enabled,
                    locale = :locale,
                    updated_at = now()
                WHERE subject = :subject
                RETURNING $PREFERENCES_COLUMNS
                """.trimIndent(),
            ).param("subject", subject)
            .param("enabled", request.emailNotificationsEnabled)
            .param("locale", request.locale)
            .query(::mapPreferences)
            .single()

    private fun mapUser(
        rs: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNum: Int,
    ): User =
        User(
            subject = rs.getString("subject"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            displayName = rs.getString("display_name"),
            fullName = rs.getString("full_name"),
            headline = rs.getString("headline"),
            phone = rs.getString("phone"),
            location = rs.getString("location"),
            linkedinUrl = rs.getString("linkedin_url"),
            githubUrl = rs.getString("github_url"),
            websiteUrl = rs.getString("website_url"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            lastSeenAt = rs.getTimestamp("last_seen_at").toInstant(),
        )

    private fun mapPreferences(
        rs: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNum: Int,
    ): UserPreferences =
        UserPreferences(
            subject = rs.getString("subject"),
            emailNotificationsEnabled = rs.getBoolean("email_notifications_enabled"),
            locale = rs.getString("locale"),
        )

    private companion object {
        const val USER_COLUMNS =
            "subject, username, email, display_name, full_name, headline, phone, location, " +
                "linkedin_url, github_url, website_url, created_at, last_seen_at"
        const val PREFERENCES_COLUMNS = "subject, email_notifications_enabled, locale"
    }
}
