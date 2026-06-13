package com.jobsearch.common.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-registers the shared web concerns on any service that has common-web on its classpath:
 * the [ProblemDetailExceptionHandler] (both stacks) and the correct correlation filter for the
 * detected web stack — [CorrelationIdServletFilter] for servlet apps, [CorrelationIdWebFilter] for
 * reactive ones.
 *
 * For structured JSON logging, services enable Spring Boot's built-in structured logging
 * (`logging.structured.format.console`); the correlation id placed in the MDC/Reactor context by the
 * filters above is then emitted on every log line.
 */
@AutoConfiguration
class CommonWebAutoConfiguration {
    @Bean
    fun problemDetailExceptionHandler(): ProblemDetailExceptionHandler = ProblemDetailExceptionHandler()

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    class ServletCorrelationConfiguration {
        @Bean
        fun correlationIdServletFilter(): CorrelationIdServletFilter = CorrelationIdServletFilter()
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    class ReactiveCorrelationConfiguration {
        @Bean
        fun correlationIdWebFilter(): CorrelationIdWebFilter = CorrelationIdWebFilter()
    }
}
