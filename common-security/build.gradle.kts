plugins {
    id("job-search.kotlin-common")
}

// OAuth2 resource-server / JWT-validation conventions shared by every backend service. Scaffolded
// now; the live Spring Security wiring (a SecurityFilterChain validating Keycloak-issued JWTs) is
// turned on in Phase 2 when Keycloak lands — see ADR 0001 / ADR 0004. Only the stable,
// framework-agnostic pieces (claim names, authority mapping) live here for now, so the module stays
// dependency-light until then.
