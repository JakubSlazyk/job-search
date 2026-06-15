package com.jobsearch.user

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/**
 * Postgres-backed user store. Upsert is keyed on the natural `subject` via `INSERT ... ON CONFLICT`,
 * so provisioning on every authenticated call is idempotent: the first call inserts, later calls just
 * refresh `username`/`email`/`last_seen_at`.
 */
@Repository
class UserRepository(
    private val jdbc: JdbcClient,
) {
    fun upsert(
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
                RETURNING subject, username, email, created_at, last_seen_at
                """.trimIndent(),
            ).param("subject", subject)
            .param("username", username)
            .param("email", email)
            .query { rs, _ ->
                User(
                    subject = rs.getString("subject"),
                    username = rs.getString("username"),
                    email = rs.getString("email"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    lastSeenAt = rs.getTimestamp("last_seen_at").toInstant(),
                )
            }.single()
}
