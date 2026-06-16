plugins {
    id("job-search.spring-service")
}

// Phase 2.3: tracker-service — a layered (controller -> service -> repository) servlet OAuth2 resource
// server. It exposes protected /api/v1/tracker/applications CRUD (the user's tracked offers, keyed on
// the Keycloak subject) and consumes `offer.published` to keep a small local denormalized copy of the
// offer fields it renders — the enrichment pattern (docs/architecture.md). Postgres + Flyway back both
// the tracked_offers and offer_snapshots tables. No gRPC (consumer + REST only).

dependencies {
    implementation(project(":common-web")) // RFC 9457 Problem Details + correlation-id filter
    implementation(project(":common-security")) // servlet resource-server SecurityFilterChain (§2.0)

    // Topics + generated OfferPublished message (consumed from the offer.published topic).
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid request bodies
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT validation
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-kafka") // offer.published enrichment consumer
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test") // @AutoConfigureMockMvc (Boot 4 module)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
}
