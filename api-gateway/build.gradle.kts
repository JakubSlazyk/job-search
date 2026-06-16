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

    // Phase 2.1 BFF (ADR 0004): the gateway is the OAuth2 client (Authorization Code + PKCE) and
    // relays the stored access token to downstream (TokenRelay). Reactive Spring Security + the
    // OAuth2 client arrive with this starter.
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // Redis-backed reactive WebSession (Spring Session): tokens stay server-side; the browser holds
    // only an httpOnly session cookie.
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.session:spring-session-data-redis")

    // In-process per-client rate limiter (Redis-backed RedisRateLimiter deferred to Phase 2 with the BFF).
    implementation(libs.resilience4j.ratelimiter)
    // Bounds the per-client RateLimiter cache (size + idle TTL) so high-cardinality client keys can't
    // grow the heap without limit. Version managed by the Spring Boot BOM.
    implementation("com.github.ben-manes.caffeine:caffeine")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux") // WebTestClient
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.wiremock) // stub downstream offer-service for routing/propagation tests
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation(libs.testcontainers.keycloak) // real Keycloak issuer for the BFF auth IT
}

// Share the single committed realm import with the Keycloak Testcontainers IT instead of duplicating
// it: copy infra/keycloak/*.json onto the test classpath under keycloak/ for withRealmImportFile.
tasks.named<Copy>("processTestResources") {
    from("$rootDir/infra/keycloak") { into("keycloak") }
}
