package com.jobsearch.common.security

/**
 * Shared conventions for the OAuth2 resource-server setup every backend service adopts in Phase 2.
 *
 * Keycloak is the identity provider; services act as resource servers and validate the JWTs it
 * issues (they never handle passwords). The Spring Security `SecurityFilterChain` that enforces this
 * is added when Keycloak is stood up (ADR 0001 / ADR 0004). Until then only the stable bits live
 * here: the claim Keycloak puts realm roles under, and how a role name maps to a Spring authority.
 */
object ResourceServerConventions {
    /** Keycloak places realm roles under this top-level JWT claim. */
    const val REALM_ACCESS_CLAIM = "realm_access"

    /** Key holding the role-name list inside the [REALM_ACCESS_CLAIM] object. */
    const val ROLES_KEY = "roles"

    /** Prefix Spring Security expects on granted authorities derived from roles. */
    const val AUTHORITY_PREFIX = "ROLE_"

    /** Maps a Keycloak role name to the corresponding Spring Security authority. */
    fun authorityOf(role: String): String = AUTHORITY_PREFIX + role
}
