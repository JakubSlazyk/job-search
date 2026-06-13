package com.jobsearch.common.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ResourceServerConventionsTest :
    StringSpec({
        "maps a Keycloak role name to a prefixed Spring Security authority" {
            ResourceServerConventions.authorityOf("admin") shouldBe "ROLE_admin"
        }
    })
