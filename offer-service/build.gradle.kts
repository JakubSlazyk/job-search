plugins {
    id("job-search.spring-service")
}

// Phase 1.5: reliable publishing. On top of the Postgres write model (§1.1) and OpenSearch read
// model (§1.4), offer-service writes an `offer.published` event to a transactional outbox in the
// same tx as the upsert; Debezium (Kafka Connect) drains it (ADR 0002). The producer is Debezium,
// not the app — so only test deps are added here.
//
// Phase 1.6: external/internal APIs over the read model — GraphQL (flexible querying), a gRPC
// `offer.v1` service for internal callers (proto in common-domain), and OpenAPI/springdoc docs on
// the REST surface. The gRPC + GraphQL server starters are managed by the Spring Boot BOM.

dependencies {
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    // Realign the whole io.grpc group to 1.81.0: spring-grpc-core forces grpc-core/-api to 1.81 but
    // Boot's grpc-bom leaves grpc-netty at 1.80, which crashes the gRPC server (see grpcRuntime in
    // the version catalog). Must come before the gRPC deps below so its constraints win.
    implementation(platform(libs.grpc.bom))
    // gRPC runtime for the generated offer.v1 service base (versions from the grpc-bom above).
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-protobuf")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql") // §1.6 GraphQL query side
    implementation("org.springframework.boot:spring-boot-starter-grpc-server") // §1.6 gRPC server (Netty, :9090)
    implementation(libs.springdoc.openapi.starter.webmvc.ui) // §1.6 OpenAPI/Swagger UI over REST
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
    testImplementation("org.springframework.boot:spring-boot-graphql-test") // §1.6 @GraphQlTest slice + GraphQlTester
    testImplementation(libs.rest.assured.spring.mock.mvc) // §1.6 RestAssured over MockMvc (no server)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(libs.testcontainers.kafka) // §1.5 full-CDC outbox -> offer.published test
    testImplementation(libs.debezium.testcontainers)
}
