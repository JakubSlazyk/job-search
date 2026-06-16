package com.jobsearch.gateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

/**
 * Stubs just enough of Keycloak's OIDC discovery document on a WireMock server so the gateway's
 * OAuth2 client registration can be built at startup (Spring fetches the well-known metadata eagerly).
 * Lets the routing/rate-limit ITs run without a real identity provider — actual login is exercised by
 * the Keycloak-backed IT and the manual/Playwright path.
 */
object OidcDiscoveryStub {
    /** Registers the discovery doc and returns the issuer URI to set as the client's issuer-uri. */
    fun register(server: WireMockServer): String {
        val issuer = "${server.baseUrl()}/realms/job-search"
        server.stubFor(
            get(urlEqualTo("/realms/job-search/.well-known/openid-configuration"))
                .willReturn(
                    okJson(
                        """
                        {
                          "issuer": "$issuer",
                          "authorization_endpoint": "$issuer/protocol/openid-connect/auth",
                          "token_endpoint": "$issuer/protocol/openid-connect/token",
                          "jwks_uri": "$issuer/protocol/openid-connect/certs",
                          "userinfo_endpoint": "$issuer/protocol/openid-connect/userinfo",
                          "end_session_endpoint": "$issuer/protocol/openid-connect/logout",
                          "response_types_supported": ["code"],
                          "subject_types_supported": ["public"],
                          "id_token_signing_alg_values_supported": ["RS256"]
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        return issuer
    }
}
