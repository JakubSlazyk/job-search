package com.jobsearch.common.web

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class CommonWebAutoConfigurationTest :
    StringSpec({
        val autoConfig = AutoConfigurations.of(CommonWebAutoConfiguration::class.java)

        "a servlet app gets the problem-detail handler and the servlet correlation filter" {
            WebApplicationContextRunner()
                .withConfiguration(autoConfig)
                .run { context ->
                    context.getBean(ProblemDetailExceptionHandler::class.java)
                    context.getBean(CorrelationIdServletFilter::class.java)
                    context.containsBean("correlationIdWebFilter") shouldBe false
                }
        }

        "a reactive app gets the problem-detail handler and the reactive correlation filter" {
            ReactiveWebApplicationContextRunner()
                .withConfiguration(autoConfig)
                .run { context ->
                    context.getBean(ProblemDetailExceptionHandler::class.java)
                    context.getBean(CorrelationIdWebFilter::class.java)
                    context.containsBean("correlationIdServletFilter") shouldBe false
                }
        }
    })
