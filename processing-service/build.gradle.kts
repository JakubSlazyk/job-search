plugins {
    id("job-search.spring-service")
}

// Phase 1.1 walking skeleton: stateless Kafka→Kafka transform raw-offers → normalized-offers.
// Source-agnostic canonicalization + dedupe and hexagonal layering arrive in §1.3.

dependencies {
    implementation(project(":common-domain"))
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-kafka")

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
}
