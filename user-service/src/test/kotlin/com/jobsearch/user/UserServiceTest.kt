package com.jobsearch.user

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class UserServiceTest :
    StringSpec({
        val repository = mockk<UserRepository>()
        val service = UserService(repository)

        fun jwt(claims: Map<String, Any>): Jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject("subject-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .also { builder -> claims.forEach { (k, v) -> builder.claim(k, v) } }
                .build()

        fun storedUser(
            subject: String,
            username: String,
            email: String?,
        ) = User(subject, username, email, Instant.now(), Instant.now())

        "upserts using preferred_username and email claims" {
            every { repository.upsert(any(), any(), any()) } answers
                { storedUser(firstArg(), secondArg(), thirdArg()) }

            val user =
                service.upsertFromToken(
                    jwt(mapOf("preferred_username" to "tester", "email" to "tester@example.com")),
                )

            user.username shouldBe "tester"
            user.email shouldBe "tester@example.com"
            verify { repository.upsert("subject-1", "tester", "tester@example.com") }
        }

        "falls back to the subject when preferred_username is absent" {
            val usernameSlot = slot<String>()
            every { repository.upsert(any(), capture(usernameSlot), any()) } answers
                { storedUser(firstArg(), usernameSlot.captured, thirdArg()) }

            service.upsertFromToken(jwt(mapOf("email" to "tester@example.com")))

            usernameSlot.captured shouldBe "subject-1"
        }
    })
