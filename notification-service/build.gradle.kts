plugins {
    id("job-search.spring-service")
}

// Phase 2.4: notification-service — the platform's reactive (Spring WebFlux + WebSockets) learning
// target (ADR 0001). It consumes `offer.published` via reactor-kafka, matches each new offer against
// user-stored criteria, resolves the user's contact over the user.v1 gRPC client, and pushes the
// match by email (Mailpit locally) and a live WebSocket. Persistence is fully reactive (R2DBC);
// Flyway runs migrations over a plain JDBC connection at startup (R2DBC can't migrate).

dependencies {
    implementation(project(":common-web")) // RFC 9457 Problem Details + correlation-id filter
    implementation(project(":common-security")) // reactive resource-server SecurityWebFilterChain (§2.0)

    // Contracts: OfferPublished (consumed) + the user.v1 gRPC stub (client). Realign io.grpc to 1.81.0
    // via grpc.bom before the gRPC deps, same as user-/offer-service — spring-grpc-core requires it.
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(platform(libs.grpc.bom))
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-protobuf")
    implementation("org.springframework.boot:spring-boot-starter-grpc-client") // user.v1 gRPC client

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-json") // Jackson (REST codecs + WebSocket payloads)
    implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid request bodies
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT validation
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc") // reactive Postgres
    implementation("org.springframework.boot:spring-boot-starter-mail") // email delivery (Mailpit/SMTP)
    implementation("org.springframework.boot:spring-boot-kafka") // KafkaProperties + kafka-clients
    implementation(libs.reactor.kafka) // reactive KafkaReceiver consumer (pinned; not in the Boot BOM)
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql") // runtime reactive driver
    runtimeOnly("org.postgresql:postgresql") // JDBC driver — Flyway migrations only

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webtestclient") // @AutoConfigureWebTestClient
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
}
