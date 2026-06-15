package com.jobsearch.common.security

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Reactive (WebFlux) counterpart of [ServletResourceServerAutoConfiguration], adopted by the
 * reactive notification-service (ADR 0001 learning target).
 *
 * Same contract: stateless, CSRF disabled (the BFF owns it), Keycloak JWT validation against the
 * auto-configured `ReactiveJwtDecoder`, and realm roles mapped via [KeycloakRealmRoleConverter]
 * (wrapped by [ReactiveJwtAuthenticationConverterAdapter] since the reactive pipeline expects a
 * `Mono`-returning converter). Guarded by [ConditionalOnWebApplication] so it only activates in
 * reactive services.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
@EnableConfigurationProperties(ResourceServerSecurityProperties::class)
class ReactiveResourceServerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityWebFilterChain::class)
    fun resourceServerSecurityWebFilterChain(
        http: ServerHttpSecurity,
        properties: ResourceServerSecurityProperties,
    ): SecurityWebFilterChain =
        http {
            csrf { disable() }
            authorizeExchange {
                properties.publicPaths.forEach { authorize(it, permitAll) }
                authorize(anyExchange, authenticated)
            }
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = reactiveJwtAuthenticationConverter() }
            }
        }

    private fun reactiveJwtAuthenticationConverter(): ReactiveJwtAuthenticationConverterAdapter {
        val delegate =
            JwtAuthenticationConverter().apply {
                setJwtGrantedAuthoritiesConverter(KeycloakRealmRoleConverter())
            }
        return ReactiveJwtAuthenticationConverterAdapter(delegate)
    }
}
