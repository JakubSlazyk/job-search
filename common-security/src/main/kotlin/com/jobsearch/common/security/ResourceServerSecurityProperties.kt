package com.jobsearch.common.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Per-service tuning for the shared resource-server filter chains.
 *
 * Everything is authenticated by default; a service opts specific routes out by listing Ant-style
 * patterns under `jobsearch.security.public-paths` (e.g. offer-service browse endpoints, actuator
 * health). The public-vs-protected route policy itself is locked in §2.1 — this just makes it
 * config-driven so no service has to re-declare a filter chain.
 */
@ConfigurationProperties(prefix = "jobsearch.security")
data class ResourceServerSecurityProperties(
    val publicPaths: List<String> = emptyList(),
)
