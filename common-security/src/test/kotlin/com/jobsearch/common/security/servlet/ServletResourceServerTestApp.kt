package com.jobsearch.common.security.servlet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Minimal servlet app that boots only the shared [ServletResourceServerAutoConfiguration] (loaded
 * via `AutoConfiguration.imports`). A stub [JwtDecoder] stands in for Keycloak so the test exercises
 * the filter chain and role mapping without a running identity provider — decoding `valid-token`
 * into a Keycloak-shaped JWT and rejecting anything else as if expired/invalid.
 */
@SpringBootApplication
open class ServletResourceServerTestApp {
    @Bean
    open fun jwtDecoder(): JwtDecoder =
        JwtDecoder { token ->
            if (token == VALID_TOKEN) {
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("tester")
                    .claim("realm_access", mapOf("roles" to listOf("user", "admin")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build()
            } else {
                throw BadJwtException("invalid or expired token")
            }
        }

    companion object {
        const val VALID_TOKEN = "valid-token"
    }
}

@RestController
class ServletTestController {
    @GetMapping("/api/me/roles")
    fun roles(authentication: JwtAuthenticationToken): List<String> =
        authentication.authorities.mapNotNull { it.authority }

    @GetMapping("/public/ping")
    fun ping(): Map<String, String> = mapOf("status" to "ok")
}
