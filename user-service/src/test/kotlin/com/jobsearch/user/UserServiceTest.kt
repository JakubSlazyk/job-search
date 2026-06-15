package com.jobsearch.user

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class UserServiceTest :
    StringSpec({
        val repository = mockk<UserRepository>()
        val service = UserService(repository)

        fun jwt(
            subject: String,
            claims: Map<String, Any> = emptyMap(),
        ): Jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .also { builder -> claims.forEach { (k, v) -> builder.claim(k, v) } }
                .build()

        fun user(
            subject: String,
            username: String = "tester",
            email: String? = "tester@example.com",
            displayName: String? = null,
        ) = User(
            subject,
            username,
            email,
            displayName,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now(),
        )

        fun prefs(
            subject: String,
            enabled: Boolean = true,
            locale: String = "en",
        ) = UserPreferences(subject, enabled, locale)

        "getMe provisions the user and default preferences from token claims" {
            every { repository.upsertUser("s1", "tester", "tester@example.com") } returns user("s1")
            every { repository.ensurePreferences("s1") } returns prefs("s1")

            val view =
                service.getMe(
                    jwt("s1", mapOf("preferred_username" to "tester", "email" to "tester@example.com")),
                )

            view.user.subject shouldBe "s1"
            view.preferences.emailNotificationsEnabled shouldBe true
            verify { repository.upsertUser("s1", "tester", "tester@example.com") }
            verify { repository.ensurePreferences("s1") }
        }

        "provision falls back to the subject when preferred_username is absent" {
            every { repository.upsertUser("s1", "s1", null) } returns user("s1", username = "s1", email = null)
            every { repository.ensurePreferences("s1") } returns prefs("s1")

            service.getMe(jwt("s1"))

            verify { repository.upsertUser("s1", "s1", null) }
        }

        "updateProfile provisions then applies the profile update" {
            every { repository.upsertUser(any(), any(), any()) } returns user("s1")
            every { repository.ensurePreferences("s1") } returns prefs("s1")
            val request = ProfileUpdateRequest("Test User", null, "Engineer", null, "Warsaw", null, null, null)
            every { repository.updateProfile("s1", request) } returns user("s1", displayName = "Test User")

            val view = service.updateProfile(jwt("s1", mapOf("preferred_username" to "tester")), request)

            view.user.displayName shouldBe "Test User"
            verify { repository.updateProfile("s1", request) }
        }

        "updatePreferences provisions then applies the preferences update" {
            every { repository.upsertUser(any(), any(), any()) } returns user("s1")
            every { repository.ensurePreferences("s1") } returns prefs("s1")
            val request = PreferencesUpdateRequest(emailNotificationsEnabled = false, locale = "pl")
            every { repository.updatePreferences("s1", request) } returns prefs("s1", enabled = false, locale = "pl")

            val view = service.updatePreferences(jwt("s1"), request)

            view.preferences.emailNotificationsEnabled shouldBe false
            view.preferences.locale shouldBe "pl"
        }

        "contactOf returns the user view when present" {
            every { repository.findUser("s1") } returns user("s1")
            every { repository.findPreferences("s1") } returns prefs("s1")

            service.contactOf("s1")?.user?.subject shouldBe "s1"
        }

        "contactOf returns null when the user is unknown" {
            every { repository.findUser("missing") } returns null

            service.contactOf("missing").shouldBeNull()
        }
    })
