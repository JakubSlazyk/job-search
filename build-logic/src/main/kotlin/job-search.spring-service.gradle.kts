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

dependencies {
    add("implementation", platform(SpringBootPlugin.BOM_COORDINATES))
}
