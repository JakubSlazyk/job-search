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

// Shared libraries (services are added in Phase 1 — see docs/build-plan.md).
include(":common-domain")
