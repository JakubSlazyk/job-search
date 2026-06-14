package com.jobsearch.offer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Duration

class OutboxPurgerTest :
    StringSpec({
        "purge deletes rows older than the retention window" {
            val outbox = mockk<OutboxRepository>()
            val cutoff = slot<Long>()
            every { outbox.deleteOlderThan(capture(cutoff)) } returns 3

            val before = System.currentTimeMillis() - Duration.ofHours(1).toMillis()
            OutboxPurger(outbox, Duration.ofHours(1)).purge()

            verify(exactly = 1) { outbox.deleteOlderThan(any()) }
            cutoff.captured shouldBeGreaterThan (before - 5_000) // ~now minus retention, allowing slack
        }

        "purge tolerates an empty outbox" {
            val outbox = mockk<OutboxRepository>()
            every { outbox.deleteOlderThan(any()) } returns 0
            OutboxPurger(outbox, Duration.ofHours(1)).purge()
            verify(exactly = 1) { outbox.deleteOlderThan(any()) }
        }
    })
