package com.jobsearch.common.security.reactive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Reactive (WebFlux) twin of the servlet test app: boots only the shared
 * [ReactiveResourceServerAutoConfiguration] with a stub [ReactiveJwtDecoder] standing in for
 * Keycloak. The slice test forces `spring.main.web-application-type=reactive` so Netty + the
 * reactive chain activate even though a servlet stack is also on the test classpath.
 */
@SpringBootApplication
open class ReactiveResourceServerTestApp {
    @Bean
    open fun reactiveJwtDecoder(): ReactiveJwtDecoder =
        ReactiveJwtDecoder { token ->
            if (token == VALID_TOKEN) {
                Mono.just(
                    Jwt
                        .withTokenValue(token)
                        .header("alg", "none")
                        .subject("tester")
                        .claim("realm_access", mapOf("roles" to listOf("user", "admin")))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build(),
                )
            } else {
                Mono.error(BadJwtException("invalid or expired token"))
            }
        }

    companion object {
        const val VALID_TOKEN = "valid-token"
    }
}

@RestController
class ReactiveTestController {
    @GetMapping("/api/me/roles")
    fun roles(): Mono<List<String>> =
        ReactiveSecurityContextHolder
            .getContext()
            .map { context -> context.authentication!!.authorities.mapNotNull { it.authority } }

    @GetMapping("/public/ping")
    fun ping(): Mono<String> = Mono.just("pong")
}
