package com.jobsearch.common.domain

/**
 * Temporary placeholder so the module compiles and the build pipeline (Kotlin, Spotless,
 * Kotest, JaCoCo) has real code to exercise. Replaced by the Protobuf-generated domain
 * model in Phase 1 (see docs/build-plan.md and ADR 0003).
 */
object BuildInfo {
    const val PROJECT = "job-search"

    fun greeting(): String = "Hello from $PROJECT"
}
