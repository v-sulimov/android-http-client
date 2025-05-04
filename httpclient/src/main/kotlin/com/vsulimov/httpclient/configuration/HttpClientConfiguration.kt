package com.vsulimov.httpclient.configuration

import java.io.InputStream

/**
 * Configuration settings for the [HttpClient].
 *
 * This class encapsulates the configuration parameters required to set up an HTTP client, including
 * timeouts, SSL certificate settings, and redirect behavior. It provides sensible defaults while
 * allowing customization through a builder pattern. Instances are immutable for thread-safety.
 *
 * @property readTimeout The maximum time (in milliseconds) to wait for data from the server after
 * a connection is established. Default: 3000 ms (3 seconds). 0 means infinite (not recommended).
 * @property connectTimeout The maximum time (in milliseconds) to wait for a connection to be
 * established. Default: 3000 ms (3 seconds). 0 means infinite (not recommended).
 * @property certificateInputStream An optional [InputStream] with an X.509 certificate for custom
 * SSL trust management (e.g., self-signed certificates). Default: `null` (system trust store only).
 * @property followRedirects Whether the HTTP client should automatically follow redirects (HTTP
 * status codes 300-399). If `true`, it follows redirects with a new GET request to the "Location"
 * header. If `false`, redirects are treated as errors. Default: `true`.
 */
class HttpClientConfiguration private constructor(
    val readTimeout: Int,
    val connectTimeout: Int,
    val certificateInputStream: InputStream?,
    val followRedirects: Boolean
) {
    /**
     * Builder for constructing [HttpClientConfiguration] instances.
     *
     * Provides a fluent API to configure settings with defaults for unspecified values.
     *
     * **Example:**
     * ```kotlin
     * val config = HttpClientConfiguration.builder()
     *     .setReadTimeout(5000)
     *     .setConnectTimeout(2000)
     *     .setCertificateInputStream(someInputStream)
     *     .setFollowRedirects(false)
     *     .build()
     * ```
     */
    class Builder {
        private var readTimeout: Int = 3000
        private var connectTimeout: Int = 3000
        private var certificateInputStream: InputStream? = null
        private var followRedirects: Boolean = true

        /**
         * Sets the read timeout (time to wait for server data after connection).
         *
         * @param readTimeout Timeout in milliseconds (non-negative).
         * @return This [Builder] for chaining.
         * @throws IllegalArgumentException If timeout is negative.
         */
        fun setReadTimeout(readTimeout: Int) = apply {
            require(readTimeout >= 0) { "Read timeout must be non-negative" }
            this.readTimeout = readTimeout
        }

        /**
         * Sets the connection timeout (time to establish a server connection).
         *
         * @param connectTimeout Timeout in milliseconds (non-negative).
         * @return This [Builder] for chaining.
         * @throws IllegalArgumentException If timeout is negative.
         */
        fun setConnectTimeout(connectTimeout: Int) = apply {
            require(connectTimeout >= 0) { "Connect timeout must be non-negative" }
            this.connectTimeout = connectTimeout
        }

        /**
         * Sets an optional certificate for custom SSL trust management.
         *
         * @param certificateInputStream [InputStream] with an X.509 certificate, or `null` for defaults.
         * @return This [Builder] for chaining.
         */
        fun setCertificateInputStream(certificateInputStream: InputStream?) = apply {
            this.certificateInputStream = certificateInputStream
        }

        /**
         * Sets whether the HTTP client should automatically follow redirects.
         *
         * @param followRedirects `true` to follow redirects, `false` to treat them as errors.
         * @return This [Builder] for chaining.
         */
        fun setFollowRedirects(followRedirects: Boolean) = apply {
            this.followRedirects = followRedirects
        }

        /**
         * Builds an immutable [HttpClientConfiguration] instance.
         *
         * @return A new [HttpClientConfiguration] with the configured settings.
         */
        fun build() = HttpClientConfiguration(readTimeout, connectTimeout, certificateInputStream, followRedirects)
    }

    companion object {
        /**
         * Creates a new [Builder] with default settings.
         *
         * @return A new [Builder] instance.
         */
        fun builder(): Builder = Builder()
    }
}
