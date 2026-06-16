package com.jobsearch.notification.grpc

import com.jobsearch.proto.user.v1.GetUserContactRequest
import com.jobsearch.proto.user.v1.UserContact
import com.jobsearch.proto.user.v1.UserContactServiceGrpc
import com.jobsearch.proto.user.v1.UserContactServiceGrpc.UserContactServiceBlockingStub
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/** Wires the blocking `user.v1` stub onto the named "user-service" channel (see application.yml). */
@Configuration
class GrpcClientConfig {
    @Bean
    fun userContactStub(channels: GrpcChannelFactory): UserContactServiceBlockingStub =
        UserContactServiceGrpc.newBlockingStub(channels.createChannel("user-service"))
}

/**
 * Reactive adapter over the blocking `user.v1` gRPC stub: resolves a user's contact by Keycloak
 * subject so the matcher knows where (and whether) to deliver. The blocking call is offloaded to the
 * bounded-elastic scheduler so it never blocks an event-loop thread. Emits empty when no user matches.
 */
@Component
class UserContactClient(
    private val stub: UserContactServiceBlockingStub,
) {
    fun resolve(subject: String): Mono<UserContact> =
        Mono
            .fromCallable {
                stub.getUserContact(GetUserContactRequest.newBuilder().setSubject(subject).build())
            }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { response ->
                if (response.hasContact()) Mono.just(response.contact) else Mono.empty()
            }
}
