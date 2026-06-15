plugins {
    id("job-search.spring-service")
}

// Phase 2.1: user-service — the first protected service in the auth walking skeleton (ADR 0004).
// A thin servlet OAuth2 resource server: it validates Keycloak-issued JWTs relayed by the gateway BFF
// (the resource-server SecurityFilterChain comes from common-security, §2.0) and exposes a single
// protected GET /api/v1/users/me. Postgres + Flyway back a users table keyed on the Keycloak subject.
// Layered architecture (controller -> service -> repository) as the deliberate style for this service.

dependencies {
    implementation(project(":common-web")) // RFC 9457 Problem Details + correlation-id filter
    implementation(project(":common-security")) // servlet resource-server SecurityFilterChain (§2.0)

    // Phase 2.2: internal user.v1 gRPC contract (proto in common-domain). Realign the io.grpc group
    // to 1.81.0 (grpc.bom) before the gRPC deps, same as offer-service — spring-grpc-core requires it.
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    implementation(platform(libs.grpc.bom))
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-protobuf")
    implementation("org.springframework.boot:spring-boot-starter-grpc-server") // user.v1 gRPC server (Netty)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid request bodies
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT validation
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test") // @AutoConfigureMockMvc (Boot 4 module)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
