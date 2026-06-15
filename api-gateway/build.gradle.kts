plugins {
    id("job-search.spring-service")
}

// Phase 1.7: api-gateway — Spring Cloud Gateway Server WebFlux (reactive/Netty). The single front
// door for the SPAs: routes the browser to offer-service (REST + GraphQL + OpenAPI), propagates the
// correlation id onto proxied requests, and rate-limits per client. The reactive variant is the
// right base for the Phase-2 BFF/Token-Relay (ADR 0004); BFF/OAuth2 + Redis-backed distributed
// rate limiting are deferred to Phase 2 — this MVP gateway uses an in-process Resilience4j limiter.

dependencies {
    // Spring Cloud BOM aligns spring-cloud-* with the release train (version from the catalog).
    implementation(platform(libs.spring.cloud.dependencies))

    implementation(project(":common-web")) // shared correlation WebFilter + RFC 9457 Problem Details
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // gateway routes + health

    // In-process per-client rate limiter (Redis-backed RedisRateLimiter deferred to Phase 2 with the BFF).
    implementation(libs.resilience4j.ratelimiter)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux") // WebTestClient
    testImplementation("io.projectreactor:reactor-test")
    testImplementation(libs.wiremock) // stub downstream offer-service for routing/propagation tests
}
