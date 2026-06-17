package com.jobsearch.collection

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.config.IntervalTask
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Confirms the polling cadence is wired to fire once every 6h. Instead of waiting on wall-clock,
 * we let Spring's scheduling post-processor register the task for [ScheduledCollector.poll] (driven by
 * `collection.polling.interval-ms`) and then introspect that task's fixed delay — deterministic, no sleep.
 */
@SpringJUnitConfig(ScheduledCollectorCadenceTest.TestConfig::class)
@TestPropertySource(properties = ["collection.polling.interval-ms=21600000"])
class ScheduledCollectorCadenceTest {
    @Autowired
    private lateinit var scheduledTasks: ScheduledTaskHolder

    @Test
    fun `poll runs on a six-hour fixed delay`() {
        val intervals =
            scheduledTasks.scheduledTasks
                .map { it.task }
                .filterIsInstance<IntervalTask>()
                .map { it.intervalDuration }

        assertEquals(listOf(Duration.ofHours(6)), intervals)
    }

    @Configuration
    @EnableScheduling
    class TestConfig {
        @Bean
        fun collector(): OfferCollector = mockk { every { collectAll() } returns Mono.empty() }

        @Bean
        fun scheduledCollector(collector: OfferCollector): ScheduledCollector = ScheduledCollector(collector)
    }
}
