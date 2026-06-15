plugins {
    id("job-search.spring-service")
}

// Phase 1.2: reactive (WebFlux) collection-service. Per-source adapters (anti-corruption layer,
// ADR 0005) fetch via WebClient and map each source's shape to a uniform RawOffer, emitted to
// `raw-offers`. Resilience4j guards outbound fetches; Jsoup parses scraped HTML.

dependencies {
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-kafka")

    implementation(libs.jsoup)
    implementation(libs.resilience4j.reactor)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation(libs.wiremock)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
}
