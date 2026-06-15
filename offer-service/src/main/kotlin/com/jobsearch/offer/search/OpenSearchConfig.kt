package com.jobsearch.offer.search

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Builds the OpenSearch Java client (Apache HttpClient 5 transport) for the read model. */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
class OpenSearchConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(destroyMethod = "close")
    fun openSearchTransport(properties: OpenSearchProperties): OpenSearchTransport {
        log.info("OpenSearch transport target {}://{}:{}", properties.scheme, properties.host, properties.port)
        return ApacheHttpClient5TransportBuilder
            .builder(HttpHost(properties.scheme, properties.host, properties.port))
            .setMapper(JacksonJsonpMapper())
            .setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder
                    // httpclient5 5.6 added async zstd/brotli content negotiation and advertises
                    // `Accept-Encoding: ...,zstd` (zstd-jni is on the classpath via Kafka). OpenSearch
                    // then zstd-compresses responses, but opensearch-java 2.25.0's transport never
                    // consumes the encoded body, so every request stalls until the response timeout.
                    // We don't need HTTP-level compression to OpenSearch, so turn it off entirely.
                    .disableContentCompression()
                    .setDefaultRequestConfig(
                        RequestConfig
                            .custom()
                            // Expect: 100-continue stalls body requests behind some proxies (Docker Desktop).
                            .setExpectContinueEnabled(false)
                            // Bound every request so a stuck call fails instead of hanging the caller forever.
                            .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                            .setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_REQUEST_TIMEOUT_SECONDS))
                            .build(),
                    )
                    // Proxies (Docker Desktop) silently drop idle keep-alive connections; reusing a dead one
                    // hangs the next read. Proactively evict idle/expired connections so reads reconnect.
                    .evictExpiredConnections()
                    .evictIdleConnections(TimeValue.ofSeconds(IDLE_EVICT_SECONDS))
            }.build()
    }

    @Bean
    fun openSearchClient(transport: OpenSearchTransport): OpenSearchClient = OpenSearchClient(transport)

    private companion object {
        const val RESPONSE_TIMEOUT_SECONDS = 30L
        const val CONNECTION_REQUEST_TIMEOUT_SECONDS = 10L
        const val IDLE_EVICT_SECONDS = 5L
    }
}
