package com.jobsearch.notification

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Points R2DBC (runtime) and Flyway (JDBC, migrations only) at the same Testcontainers Postgres.
 * Shared by the integration tests so the dual-URL wiring lives in one place.
 */
fun DynamicPropertyRegistry.registerPostgres(postgres: PostgreSQLContainer<*>) {
    val r2dbcUrl = "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
    add("spring.r2dbc.url") { r2dbcUrl }
    add("spring.r2dbc.username") { postgres.username }
    add("spring.r2dbc.password") { postgres.password }
    add("spring.flyway.url") { postgres.jdbcUrl }
    add("spring.flyway.user") { postgres.username }
    add("spring.flyway.password") { postgres.password }
}
