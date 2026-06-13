plugins {
    id("job-search.kotlin-common")
}

// Shared web concerns reused by every HTTP-exposing service: RFC 9457 Problem Details, a
// correlation-ID filter (servlet + WebFlux variants), and correlation-aware logging. Delivered as a
// Spring Boot auto-configuration, so a service gets it just by having this module on the classpath.
//
// Spring / Reactor / Servlet APIs are compileOnly: each service brings its own runtime (a servlet
// service must not drag in WebFlux, and vice versa). Versions come from the Spring Boot BOM.

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-web")
    compileOnly("io.projectreactor:reactor-core")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.slf4j:slf4j-api")

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-webmvc") // servlet web context for the auto-config runner
    testImplementation("org.springframework:spring-webflux") // satisfies @ConditionalOnWebApplication(REACTIVE)
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.slf4j:slf4j-api")
    testImplementation("org.assertj:assertj-core") // AssertableApplicationContext extends an AssertJ type
    testRuntimeOnly("ch.qos.logback:logback-classic") // real SLF4J MDC adapter (slf4j alone no-ops MDC)
}
