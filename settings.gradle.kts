rootProject.name = "job-search"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Fail the build if a subproject declares its own repositories — keep them centralized here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    // gradle/libs.versions.toml is picked up automatically as the `libs` catalog.
}

// Shared libraries.
include(":common-domain")
include(":common-web")
include(":common-security")
include(":common-archtest")

// Business services — added per subphase (see docs/phase-1-plan.md).
// Phase 1.1 walking skeleton: the thin collection → processing → offer-service slice.
include(":collection-service")
include(":processing-service")
include(":offer-service")
