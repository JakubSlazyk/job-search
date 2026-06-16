package com.jobsearch.notification.ws

import com.jobsearch.notification.DeliveredNotification
import com.jobsearch.notification.delivery.NotificationStream
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.reactivestreams.Publisher
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tools.jackson.databind.ObjectMapper
import java.security.Principal
import java.time.Duration
import java.time.Instant

class NotificationWebSocketHandlerTest :
    StringSpec({
        "streams only the authenticated subject's notifications to the session" {
            val stream = NotificationStream()
            val handler = NotificationWebSocketHandler(stream, ObjectMapper())

            val principal = mockk<Principal>()
            every { principal.name } returns "s1"
            val info = mockk<HandshakeInfo>()
            every { info.principal } returns Mono.just(principal)
            val sent = slot<Publisher<WebSocketMessage>>()
            val session = mockk<WebSocketSession>()
            every { session.handshakeInfo } returns info
            every { session.textMessage(any()) } returns mockk()
            every { session.send(capture(sent)) } returns Mono.empty()

            StepVerifier.create(handler.handle(session)).verifyComplete()

            StepVerifier
                .create(Flux.from(sent.captured))
                .then {
                    stream.publish(notification("other"))
                    stream.publish(notification("s1"))
                }.expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }
    })

private fun notification(subject: String) =
    DeliveredNotification(1, subject, "sample:1", "Kotlin", "ACME", "https://example.com/1", Instant.now())
