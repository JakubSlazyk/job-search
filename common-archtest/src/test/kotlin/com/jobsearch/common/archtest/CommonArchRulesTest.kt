package com.jobsearch.common.archtest

import com.tngtech.archunit.core.importer.ClassFileImporter
import io.kotest.core.spec.style.StringSpec

class CommonArchRulesTest :
    StringSpec({
        // Self-test: the baseline rules must hold for this module's own classes. Once Phase 1
        // services exist, they run the same rules against their packages.
        val classes = ClassFileImporter().importPackages("com.jobsearch.common.archtest")

        "baseline architecture rules hold for this module" {
            CommonArchRules.assertAll(classes)
        }
    })
