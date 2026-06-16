package com.jobsearch.common.security

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Servlet OAuth2 resource-server chain, adopted by offer-/user-/tracker-service.
 *
 * Validates Keycloak-issued JWTs (the `JwtDecoder` is auto-configured by Spring Boot from
 * `spring.security.oauth2.resourceserver.jwt.issuer-uri`), maps realm roles via
 * [KeycloakRealmRoleConverter], and is **stateless**: no server-side session, CSRF disabled —
 * session and CSRF concerns live in the gateway BFF, not the resource servers (ADR 0004). Public
 * routes come from [ResourceServerSecurityProperties]; everything else requires a valid token.
 *
 * Registered as an auto-configuration (see `AutoConfiguration.imports`) and guarded by
 * [ConditionalOnWebApplication] so it only activates in servlet services. [ConditionalOnMissingBean]
 * lets a service still override the chain entirely if it needs to.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableConfigurationProperties(ResourceServerSecurityProperties::class)
class ServletResourceServerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    fun resourceServerSecurityFilterChain(
        http: HttpSecurity,
        properties: ResourceServerSecurityProperties,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                properties.publicPaths.forEach { authorize(it, permitAll) }
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = jwtAuthenticationConverter() }
            }
        }
        return http.build()
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(KeycloakRealmRoleConverter())
        }
}
