plugins {
    id("job-search.spring-service")
}

// Phase 1.4: CQRS read side. offer-service keeps the Postgres write model (§1.1) and adds an
// OpenSearch read model — indexing on upsert and serving browse/search/filter from it. Outbox +
// Debezium (§1.5) and GraphQL/gRPC (§1.6) layer on later.

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
}
