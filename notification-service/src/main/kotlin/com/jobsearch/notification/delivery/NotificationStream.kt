package com.jobsearch.notification.delivery

import com.jobsearch.notification.DeliveredNotification
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration

/**
 * In-process multicast hub for live WebSocket delivery. Matches are emitted once and each connected
 * session subscribes to the stream filtered to its own subject. A single replicated instance is fine
 * for the local/portfolio scope; a multi-instance deployment would back this with a shared broker.
 */
@Component
class NotificationStream {
    private val sink = Sinks.many().multicast().onBackpressureBuffer<DeliveredNotification>(256, false)

    /**
     * Emit a delivered match to any connected sessions. Deliveries for one event run concurrently, so
     * emission is serialized with a short busy-loop on `FAIL_NON_SERIALIZED` rather than the racy
     * `tryEmitNext` (whose failure result would be silently dropped). Best-effort: drops if nothing
     * is listening.
     */
    fun publish(notification: DeliveredNotification) {
        sink.emitNext(notification, Sinks.EmitFailureHandler.busyLooping(BUSY_LOOP_TIMEOUT))
    }

    /** Live notifications for one subject — the per-session WebSocket feed. */
    fun forSubject(subject: String): Flux<DeliveredNotification> = sink.asFlux().filter { it.subject == subject }

    private companion object {
        val BUSY_LOOP_TIMEOUT: Duration = Duration.ofSeconds(1)
    }
}
