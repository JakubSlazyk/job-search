plugins {
    id("job-search.spring-service")
}

// Phase 1.1 walking skeleton: collection-service emits a sample RawOffer to `raw-offers`.
// Reactive/WebFlux conversion and real per-source adapters (ADR 0005) arrive in §1.2.

dependencies {
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-kafka")

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
}
