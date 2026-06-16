package com.jobsearch.notification.delivery

import com.jobsearch.notification.DeliveredNotification
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * In-process multicast hub for live WebSocket delivery. Matches are emitted once and each connected
 * session subscribes to the stream filtered to its own subject. A single replicated instance is fine
 * for the local/portfolio scope; a multi-instance deployment would back this with a shared broker.
 */
@Component
class NotificationStream {
    private val sink = Sinks.many().multicast().onBackpressureBuffer<DeliveredNotification>(256, false)

    /** Emit a delivered match to any connected sessions. Best-effort: drops if nothing is listening. */
    fun publish(notification: DeliveredNotification) {
        sink.tryEmitNext(notification)
    }

    /** Live notifications for one subject — the per-session WebSocket feed. */
    fun forSubject(subject: String): Flux<DeliveredNotification> = sink.asFlux().filter { it.subject == subject }
}
