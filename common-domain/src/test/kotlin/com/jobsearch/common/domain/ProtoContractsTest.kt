package com.jobsearch.common.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtoContractsTest :
    StringSpec({
        "newPing builds a Ping carrying the given message" {
            ProtoContracts.newPing("pong").message shouldBe "pong"
        }
    })
