package com.jobsearch.common.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Extracts Keycloak realm roles from a validated JWT and maps them to Spring Security authorities.
 *
 * Keycloak nests realm roles as `realm_access.roles` (see [ResourceServerConventions]); each role is
 * exposed as a `ROLE_`-prefixed authority so it works with `hasRole(...)` checks. A token without the
 * claim simply yields no authorities rather than failing — authentication still succeeds, the
 * principal just carries no roles. Shared by both the servlet and reactive resource-server chains.
 */
class KeycloakRealmRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val roles =
            jwt
                .getClaimAsMap(ResourceServerConventions.REALM_ACCESS_CLAIM)
                ?.get(ResourceServerConventions.ROLES_KEY) as? Collection<*>
        return roles
            ?.filterIsInstance<String>()
            ?.map { SimpleGrantedAuthority(ResourceServerConventions.authorityOf(it)) }
            ?: emptyList()
    }
}
