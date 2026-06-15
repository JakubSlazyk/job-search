import org.gradle.accessors.dm.LibrariesForLibs
import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Spring Boot application conventions, layered on job-search.kotlin-common. Applied by every
// deployable service (first consumer: collection-service in Phase 1.1). The Spring Boot BOM is
// imported as a platform so service build scripts can declare starters without versions.
//
// kotlin("plugin.spring") opens Spring-annotated classes (all-open) so Kotlin's final-by-default
// classes work with Spring proxying.

plugins {
    id("job-search.kotlin-common")
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

val libs = the<LibrariesForLibs>()

dependencies {
    add("implementation", platform(SpringBootPlugin.BOM_COORDINATES))
    // kotlin-reflect is required by Spring Boot's Kotlin support: @ConfigurationProperties
    // constructor binding (e.g. binding OPENSEARCH_HOST onto an immutable data class) and the
    // Jackson Kotlin module both reflect over Kotlin metadata. Pin to the project's Kotlin version
    // so it matches the compiler rather than whatever the Spring BOM happens to manage.
    add("implementation", "org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
}
