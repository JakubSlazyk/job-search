package com.jobsearch.user.grpc

import com.jobsearch.proto.user.v1.GetUserContactRequest
import com.jobsearch.proto.user.v1.GetUserContactResponse
import com.jobsearch.proto.user.v1.UserContact
import com.jobsearch.proto.user.v1.UserContactServiceGrpc
import com.jobsearch.user.UserService
import com.jobsearch.user.UserView
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service

/**
 * Internal `user.v1` gRPC server: resolves a user's contact details by Keycloak subject for
 * notification-service (§2.4). Auto-registered with the Spring Boot gRPC server via `@Service`.
 * `contact` is left unset in the response when the subject is unknown.
 */
@Service
class UserGrpcService(
    private val userService: UserService,
) : UserContactServiceGrpc.UserContactServiceImplBase() {
    override fun getUserContact(
        request: GetUserContactRequest,
        responseObserver: StreamObserver<GetUserContactResponse>,
    ) {
        val response = GetUserContactResponse.newBuilder()
        userService.contactOf(request.subject)?.let { response.contact = it.toContact() }
        responseObserver.onNext(response.build())
        responseObserver.onCompleted()
    }
}

private fun UserView.toContact(): UserContact =
    UserContact
        .newBuilder()
        .setSubject(user.subject)
        .setEmail(user.email ?: "")
        .setDisplayName(user.displayName ?: user.username)
        .setEmailNotificationsEnabled(preferences.emailNotificationsEnabled)
        .build()
