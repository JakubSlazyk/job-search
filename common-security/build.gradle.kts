import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("job-search.kotlin-common")
}

// OAuth2 resource-server / JWT-validation conventions shared by every backend service (ADR 0001 /
// ADR 0004). Phase 2.0 turns on the real Spring Security wiring: a servlet and a reactive
// resource-server filter chain that validate Keycloak-issued JWTs and map realm roles to Spring
// authorities. Registered as Spring Boot auto-configurations, so a service opts in just by depending
// on this module and setting `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
//
// The Spring/Spring Security artifacts are `compileOnly`: this stays a plain library (no Boot app
// plugin), and each consumer brings its own web stack — servlet services pull the
// oauth2-resource-server starter, the reactive notification-service pulls WebFlux + security. Only
// the matching auto-configuration activates (`@ConditionalOnWebApplication`). The Spring Boot BOM
// aligns the versions so no security version lives in the catalog.

dependencies {
    val bom = platform(SpringBootPlugin.BOM_COORDINATES)
    compileOnly(bom)

    // Compile-time only: both web/security stacks are present so both filter-chain variants compile,
    // but neither is forced onto consumers at runtime.
    // servlet JWT resource server
    compileOnly("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    // reactive ServerHttpSecurity
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    // @AutoConfiguration / @ConditionalOnWebApplication
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Tests exercise both variants, so they need the full stack on the test runtime classpath.
    testImplementation(bom)
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
}
