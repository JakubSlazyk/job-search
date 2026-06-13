package com.jobsearch.common.archtest

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Baseline architecture rules every service can assert against its own classes. Phase 1 services add
 * service-specific layering rules (hexagonal / onion) on top of these.
 */
object CommonArchRules {
    /** Constructor injection only — no `@Autowired` on fields (testability, immutability). */
    val noFieldInjection: ArchRule =
        fields()
            .should()
            .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("prefer constructor injection over field injection")

    /** No direct use of the standard streams — logging goes through SLF4J. */
    val noStandardStreams: ArchRule =
        noClasses()
            .should()
            .accessField(System::class.java, "out")
            .orShould()
            .accessField(System::class.java, "err")
            .because("use SLF4J structured logging, not System.out/err")

    /** Asserts every baseline rule against the given imported classes. */
    fun assertAll(classes: JavaClasses) {
        noFieldInjection.check(classes)
        noStandardStreams.check(classes)
    }
}
