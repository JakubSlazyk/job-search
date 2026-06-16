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

// Phase 1.7: Spring Cloud Gateway — single entry point routing the browser to offer-service
// (REST + GraphQL), with correlation-ID propagation and in-process rate limiting. BFF/OAuth2
// deferred to Phase 2 (see docs/phase-1-plan.md §1.7, ADR 0004).
include(":api-gateway")

// Phase 2.1: user-service — first protected service in the auth walking skeleton (servlet resource
// server, GET /api/v1/users/me). See docs/phase-2-plan.md §2.1, ADR 0004.
include(":user-service")

// Phase 2.3: tracker-service — layered application tracker (protected REST CRUD) that keeps a local
// denormalized copy of offer fields by consuming `offer.published` (enrichment). See docs/phase-2-plan.md §2.3.
include(":tracker-service")

// Phase 2.4: notification-service — reactive (WebFlux + WebSockets) matcher. Consumes `offer.published`
// (reactor-kafka), matches user criteria, resolves contacts via user.v1 gRPC, and pushes via email
// (Mailpit) + a live WebSocket. R2DBC persistence. See docs/phase-2-plan.md §2.4, ADR 0001.
include(":notification-service")
