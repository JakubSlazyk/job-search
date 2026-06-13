plugins {
    id("job-search.kotlin-common")
    `java-test-fixtures`
}

// Reusable ArchUnit rules shared across services. A service consumes them from its own tests via:
//   testImplementation(testFixtures(project(":common-archtest")))
// and applies e.g. CommonArchRules.assertAll(importedClasses). Phase 1 services layer their own
// hexagonal / onion rules on top. No business services exist yet, so the rules are exercised here by
// this module's self-test against its own classes.

dependencies {
    // `api` so the rules' ArchUnit types are visible to consumers (and to this module's test).
    testFixturesApi(libs.archunit.junit5)
}
