package com.jobsearch.notification.grpc

import com.jobsearch.proto.user.v1.GetUserContactRequest
import com.jobsearch.proto.user.v1.GetUserContactResponse
import com.jobsearch.proto.user.v1.UserContact
import com.jobsearch.proto.user.v1.UserContactServiceGrpc.UserContactServiceBlockingStub
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.test.StepVerifier

class UserContactClientTest :
    StringSpec({
        val stub = mockk<UserContactServiceBlockingStub>()
        val client = UserContactClient(stub)

        "emits the contact when the user exists" {
            every { stub.getUserContact(any<GetUserContactRequest>()) } returns
                GetUserContactResponse
                    .newBuilder()
                    .setContact(
                        UserContact
                            .newBuilder()
                            .setSubject("s1")
                            .setEmail("s1@example.com")
                            .build(),
                    ).build()

            StepVerifier
                .create(client.resolve("s1"))
                .assertNext { it.email shouldBe "s1@example.com" }
                .verifyComplete()
        }

        "emits empty when the user is unknown" {
            every { stub.getUserContact(any<GetUserContactRequest>()) } returns
                GetUserContactResponse.getDefaultInstance()

            StepVerifier.create(client.resolve("missing")).verifyComplete()
        }
    })
