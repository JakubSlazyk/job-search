package com.jobsearch.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.security.web.server.csrf.CsrfWebFilter
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono

/**
 * Backend-for-Frontend security (ADR 0004). The gateway is the OAuth2 **client**: it runs
 * Authorization Code + PKCE against Keycloak, keeps the tokens server-side in a Redis-backed
 * `WebSession`, and hands the browser only an httpOnly session cookie. The access token is relayed
 * downstream by the `TokenRelay` default filter (see application.yml).
 *
 * Route policy locked for the rest of Phase 2: offer browse (+ docs, health, OAuth2 endpoints) is
 * public; everything else — notably the user routes — requires an authenticated session.
 *
 * This chain is independent of [GatewayConfiguration]'s correlation/rate-limit global filters: those
 * run only for matched routes, while the OAuth2/login endpoints are handled here.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
class SecurityConfiguration(
    // The SPA origin the BFF returns the browser to after login/logout. The gateway serves no UI of
    // its own, so without this the default success handler would redirect to the gateway's own `/`
    // (404 → 500). In dev the SPA is the Vite server (:5173); override FRONTEND_BASE_URL elsewhere.
    @Value("\${app.frontend-base-url:http://localhost:5173/}")
    private val frontendBaseUrl: String,
) {
    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        clientRegistrations: ReactiveClientRegistrationRepository,
    ): SecurityWebFilterChain =
        http {
            authorizeExchange {
                authorize("/actuator/health/**", permitAll)
                authorize("/api/v1/offers/**", permitAll)
                authorize("/graphql", permitAll)
                authorize("/graphiql", permitAll)
                authorize("/graphiql/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-ui/**", permitAll)
                // OAuth2 login + callback endpoints must stay open to bootstrap a session.
                authorize("/oauth2/**", permitAll)
                authorize("/login/**", permitAll)
                authorize(anyExchange, authenticated)
            }
            // After a successful login the browser is on the gateway callback origin; send it back to
            // the SPA (the gateway hosts no UI). Without this the default handler targets the
            // gateway's own `/`, which has no resource (404 → 500).
            oauth2Login {
                authenticationSuccessHandler = RedirectServerAuthenticationSuccessHandler(frontendBaseUrl)
            }
            logout { logoutSuccessHandler = oidcLogoutSuccessHandler(clientRegistrations) }
            // Cookie-based CSRF so the SPA can read XSRF-TOKEN and echo it on mutating calls. The
            // token is httpOnly=false on purpose (the SPA must read it); see csrfCookieWebFilter.
            // The plain request handler resolves the *raw* token value: the reactive default
            // (XorServerCsrfTokenRequestAttributeHandler) masks the token, which mismatches the raw
            // value the SPA reads from the cookie and submits — every SPA mutation (incl. /logout)
            // would 403. Protect only mutating methods on non-public paths — public browse/query
            // routes (e.g. the GraphQL POST query endpoint) stay usable without a token.
            csrf {
                csrfTokenRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                csrfTokenRequestHandler = ServerCsrfTokenRequestAttributeHandler()
                requireCsrfProtectionMatcher =
                    AndServerWebExchangeMatcher(
                        CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                        NegatedServerWebExchangeMatcher(publicPathsMatcher()),
                    )
            }
        }

    /** Public surface (browse/query, docs, health, OAuth2 endpoints) — exempt from CSRF and auth. */
    private fun publicPathsMatcher(): ServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(
            "/api/v1/offers/**",
            "/graphql",
            "/graphiql/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/oauth2/**",
            "/login/**",
        )

    /** RP-initiated logout: end the Keycloak session too, then return to the SPA. */
    private fun oidcLogoutSuccessHandler(
        clientRegistrations: ReactiveClientRegistrationRepository,
    ): ServerLogoutSuccessHandler =
        OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrations).apply {
            setPostLogoutRedirectUri(frontendBaseUrl)
        }

    /**
     * Forces the deferred [CsrfToken] to be resolved so its cookie is actually written on the
     * response — the well-known WebFlux gotcha with cookie-based CSRF behind a BFF.
     */
    @Bean
    fun csrfCookieWebFilter(): WebFilter =
        WebFilter { exchange, chain ->
            exchange.response.beforeCommit {
                Mono.defer {
                    val token = exchange.getAttribute<Mono<CsrfToken>>(CsrfToken::class.java.name)
                    token?.then() ?: Mono.empty()
                }
            }
            chain.filter(exchange)
        }
}
