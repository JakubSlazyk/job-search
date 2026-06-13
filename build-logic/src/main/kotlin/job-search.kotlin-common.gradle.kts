import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// Base convention plugin shared by every JVM module: Kotlin (JDK 25 toolchain),
// Kotest on the JUnit platform, Spotless/ktlint formatting, detekt static analysis,
// and the JaCoCo 80% gate.

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("dev.detekt")
    jacoco
}

val libs = the<LibrariesForLibs>()

group = "com.jobsearch"
version = "0.0.1-SNAPSHOT"

// Repositories are centralized in settings.gradle.kts (FAIL_ON_PROJECT_REPOS).

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)
}

spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        target("src/**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
    }
}

// detekt owns static analysis / code smells. Formatting is left to Spotless/ktlint, so the
// optional detekt-formatting ruleset is intentionally not added.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    // Print a one-line totals summary per module once its test suite finishes.
    addTestListener(
        object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) = Unit
            override fun beforeTest(test: TestDescriptor) = Unit
            override fun afterTest(test: TestDescriptor, result: TestResult) = Unit

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) {
                    logger.lifecycle(
                        "Test results: ${result.resultType} - " +
                            "${result.testCount} tests, " +
                            "${result.successfulTestCount} passed, " +
                            "${result.failedTestCount} failed, " +
                            "${result.skippedTestCount} skipped",
                    )
                }
            }
        },
    )
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
