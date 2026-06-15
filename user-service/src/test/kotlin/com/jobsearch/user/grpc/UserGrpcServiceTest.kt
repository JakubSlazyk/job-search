package com.jobsearch.user.grpc

import com.jobsearch.proto.user.v1.GetUserContactRequest
import com.jobsearch.proto.user.v1.GetUserContactResponse
import com.jobsearch.user.User
import com.jobsearch.user.UserPreferences
import com.jobsearch.user.UserService
import com.jobsearch.user.UserView
import io.grpc.stub.StreamObserver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

/**
 * Unit-tests the `user.v1` gRPC service against its generated base (real proto messages, service
 * layer mocked) by invoking the unary method directly with a recording [StreamObserver] — no
 * transport, mirroring offer-service's gRPC test.
 */
class UserGrpcServiceTest :
    StringSpec({
        fun view(
            email: String? = "tester@example.com",
            displayName: String? = null,
            emailNotifications: Boolean = true,
        ) = UserView(
            User(
                "s1",
                "tester",
                email,
                displayName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
            ),
            UserPreferences("s1", emailNotifications, "en"),
        )

        "GetUserContact maps the contact when found, falling back to the username for display name" {
            val service = mockk<UserService> { every { contactOf("s1") } returns view() }
            val observer = RecordingObserver<GetUserContactResponse>()

            UserGrpcService(service).getUserContact(
                GetUserContactRequest.newBuilder().setSubject("s1").build(),
                observer,
            )

            val response = observer.single()
            response.hasContact() shouldBe true
            response.contact.subject shouldBe "s1"
            response.contact.email shouldBe "tester@example.com"
            response.contact.displayName shouldBe "tester"
            response.contact.emailNotificationsEnabled shouldBe true
        }

        "GetUserContact leaves the contact unset when the subject is unknown" {
            val service = mockk<UserService> { every { contactOf("missing") } returns null }
            val observer = RecordingObserver<GetUserContactResponse>()

            UserGrpcService(service).getUserContact(
                GetUserContactRequest.newBuilder().setSubject("missing").build(),
                observer,
            )

            observer.single().hasContact() shouldBe false
        }
    })

private class RecordingObserver<T> : StreamObserver<T> {
    private val values = mutableListOf<T>()
    private var completed = false

    override fun onNext(value: T) {
        values += value
    }

    override fun onError(t: Throwable): Unit = throw t

    override fun onCompleted() {
        completed = true
    }

    fun single(): T {
        check(completed) { "stream not completed" }
        return values.single()
    }
}
