package com.jobsearch.common.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BuildInfoTest :
    StringSpec({
        "greeting includes the project name" {
            BuildInfo.greeting() shouldBe "Hello from job-search"
        }
    })
