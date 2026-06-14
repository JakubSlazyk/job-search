plugins {
    id("job-search.spring-service")
}

// Phase 1.5: reliable publishing. On top of the Postgres write model (§1.1) and OpenSearch read
// model (§1.4), offer-service writes an `offer.published` event to a transactional outbox in the
// same tx as the upsert; Debezium (Kafka Connect) drains it (ADR 0002). The producer is Debezium,
// not the app — so only test deps are added here. GraphQL/gRPC (§1.6) layer on later.

dependencies {
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-kafka")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(libs.opensearch.java)
    implementation("org.apache.httpcomponents.client5:httpclient5") // OpenSearch client transport
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(libs.testcontainers.kafka) // §1.5 full-CDC outbox -> offer.published test
    testImplementation(libs.debezium.testcontainers)
}
