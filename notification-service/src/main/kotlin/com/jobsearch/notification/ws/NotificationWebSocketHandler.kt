package com.jobsearch.notification.ws

import com.jobsearch.notification.NotificationResponse
import com.jobsearch.notification.delivery.NotificationStream
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

/**
 * Live notification feed over WebFlux WebSockets. The handshake goes through the §2.0 reactive
 * resource-server chain, so the session principal is the authenticated JWT — its name is the Keycloak
 * subject. Each session is sent only its own subject's matches from the in-process [NotificationStream].
 */
@Component
class NotificationWebSocketHandler(
    private val stream: NotificationStream,
    private val objectMapper: ObjectMapper,
) : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> =
        session.handshakeInfo.principal
            .flatMap { principal ->
                val messages =
                    stream
                        .forSubject(principal.name)
                        .map { session.textMessage(objectMapper.writeValueAsString(NotificationResponse.from(it))) }
                session.send(messages)
            }
}

/** Maps the WebSocket handler ahead of annotated controllers; the adapter is auto-configured by WebFlux. */
@Configuration
class WebSocketConfig {
    @Bean
    fun notificationHandlerMapping(handler: NotificationWebSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/notifications" to handler), Ordered.HIGHEST_PRECEDENCE)
}
