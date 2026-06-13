package com.jobsearch.processing

import com.jobsearch.common.archtest.CommonArchRules
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import io.kotest.core.spec.style.StringSpec

class ProcessingArchitectureTest :
    StringSpec({
        val classes =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.jobsearch.processing")

        "baseline architecture rules hold" {
            CommonArchRules.assertAll(classes)
        }

        "hexagonal layering is respected" {
            layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("com.jobsearch.processing.domain..")
                .layer("Application")
                .definedBy("com.jobsearch.processing.application..")
                .layer("Adapter")
                .definedBy("com.jobsearch.processing.adapter..")
                .whereLayer("Adapter")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Adapter")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Adapter")
                .check(classes)
        }

        "domain and application stay framework-free" {
            noClasses()
                .that()
                .resideInAnyPackage("..domain..", "..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .check(classes)
        }
    })
