package com.jobsearch.notification.delivery

import com.jobsearch.notification.OfferMatch
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import reactor.test.StepVerifier

class EmailSenderTest :
    StringSpec({
        "sends a message carrying the match details from the configured address" {
            val mailSender = mockk<MailSender>()
            val captured = slot<SimpleMailMessage>()
            every { mailSender.send(capture(captured)) } just Runs
            val sender = EmailSender(mailSender, "notifications@job-search.local")

            val match = OfferMatch("sample:1", "Kotlin Engineer", "ACME", "https://example.com/1")
            StepVerifier
                .create(sender.send("sam@example.com", "Sam", match))
                .verifyComplete()

            captured.captured.from shouldBe "notifications@job-search.local"
            captured.captured.to!!.first() shouldBe "sam@example.com"
            captured.captured.subject!! shouldContain "Kotlin Engineer"
            captured.captured.text!! shouldContain "ACME"
        }
    })
