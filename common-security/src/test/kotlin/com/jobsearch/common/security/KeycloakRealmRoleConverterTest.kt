package com.jobsearch.common.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class KeycloakRealmRoleConverterTest :
    StringSpec({
        val converter = KeycloakRealmRoleConverter()

        fun jwt(claims: Map<String, Any>): Jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject("tester")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .also { builder -> claims.forEach { (k, v) -> builder.claim(k, v) } }
                .build()

        "maps realm_access roles to ROLE_-prefixed authorities" {
            val authorities =
                converter.convert(
                    jwt(mapOf("realm_access" to mapOf("roles" to listOf("user", "admin")))),
                )

            authorities.map { it.authority } shouldContainExactlyInAnyOrder listOf("ROLE_user", "ROLE_admin")
        }

        "returns no authorities when the realm_access claim is absent" {
            converter.convert(jwt(mapOf("sub" to "tester"))) shouldBe emptyList()
        }

        "returns no authorities when realm_access has no roles list" {
            converter.convert(jwt(mapOf("realm_access" to mapOf("other" to "x")))) shouldBe emptyList()
        }

        "ignores non-string entries in the roles list" {
            val authorities =
                converter.convert(
                    jwt(mapOf("realm_access" to mapOf("roles" to listOf("user", 42, null)))),
                )

            authorities.map { it.authority } shouldContainExactlyInAnyOrder listOf("ROLE_user")
        }
    })
