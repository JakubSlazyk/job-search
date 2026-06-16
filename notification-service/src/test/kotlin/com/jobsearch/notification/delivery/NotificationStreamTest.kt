package com.jobsearch.notification.delivery

import com.jobsearch.notification.DeliveredNotification
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant

class NotificationStreamTest :
    StringSpec({
        fun notification(subject: String) =
            DeliveredNotification(1, subject, "sample:1", "Kotlin", "ACME", "https://example.com/1", Instant.now())

        "delivers only the subscribing subject's notifications" {
            val stream = NotificationStream()

            StepVerifier
                .create(stream.forSubject("s1"))
                .then {
                    stream.publish(notification("other"))
                    stream.publish(notification("s1"))
                }.assertNext { it.subject shouldBe "s1" }
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }
    })
