package com.jobsearch.offer

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/springdoc metadata for the REST surface (§1.6). springdoc scans the annotated controllers
 * and serves the spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. GraphQL and gRPC are
 * documented by their own schemas/protos, so this describes only the REST read API.
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun offerServiceOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("offer-service API")
                .version("v1")
                .description(
                    "Browse and search canonical job offers (REST read model; GraphQL and gRPC also available).",
                ),
        )
}
