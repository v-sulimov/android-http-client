package com.vsulimov.httpclient.security

import android.annotation.SuppressLint
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * A composite [X509TrustManager] that combines multiple trust managers for SSL certificate validation.
 *
 * The [CompositeX509TrustManager] class implements the [X509TrustManager] interface to provide a flexible trust management
 * mechanism for SSL/TLS connections. It aggregates multiple trust managers—typically the system's default trust manager
 * plus one or more custom trust managers backed by provided [KeyStore] instances—into a single entity. This allows the
 * HTTP client to trust certificates from both the system’s default trust store and additional custom certificates (e.g.,
 * self-signed or custom CA certificates).
 *
 * ### Purpose
 * - To enable the HTTP client to handle SSL connections with custom certificates while retaining default system trust.
 * - To attempt validation with multiple trust managers, falling back until one succeeds or all fail.
 *
 * ### Constructors
 * - One constructor accepts a single [KeyStore] for a custom certificate.
 * - Another constructor accepts a [List] of [KeyStore] instances for multiple custom certificates.
 *
 * ### Behavior
 * - Combines the default system trust manager with custom trust managers derived from provided keystores.
 * - During certificate validation (`checkClientTrusted` or `checkServerTrusted`), it iterates through trust managers,
 *   succeeding if any one trusts the certificate chain, or failing if none do.
 * - Returns a union of accepted issuers from all trust managers via `getAcceptedIssuers`.
 *
 * ### Usage
 * This class is typically used to initialize an [SSLContext] for HTTPS connections in an HTTP client, allowing it to
 * trust both system and custom certificates.
 *
 * **Example:**
 * ```kotlin
 * val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
 *     load(null, null)
 *     setCertificateEntry("custom-ca", customCertificate)
 * }
 * val trustManager = CompositeX509TrustManager(keyStore)
 * val sslContext = SSLContext.getInstance("TLS")
 * sslContext.init(null, arrayOf(trustManager), null)
 * // Use sslContext.socketFactory in HttpsURLConnection
 * ```
 *
 * ### Notes
 * - If no trust manager accepts a certificate chain, a [CertificateException] is thrown.
 * - Exceptions from individual trust managers are silently ignored until all are exhausted, which may obscure specific
 *   failure reasons—consider logging these for debugging.
 * - The class assumes the first [X509TrustManager] in a [TrustManagerFactory] is the relevant one; this is typically safe
 *   but depends on the JVM implementation.
 */
@SuppressLint("CustomX509TrustManager")
class CompositeX509TrustManager : X509TrustManager {

    /** A mutable list of [X509TrustManager] instances used for certificate validation. */
    private val trustManagers: MutableList<X509TrustManager>

    /**
     * Constructs a [CompositeX509TrustManager] with the default trust manager and one custom trust manager.
     *
     * @param keyStore A [KeyStore] containing custom certificates to trust (e.g., self-signed or custom CA certificates).
     */
    constructor(keyStore: KeyStore) {
        trustManagers = mutableListOf(
            getDefaultTrustManager(),
            getTrustManagerForKeyStore(keyStore)
        )
    }

    /**
     * Constructs a [CompositeX509TrustManager] with the default trust manager and multiple custom trust managers.
     *
     * @param keyStores A [List] of [KeyStore] instances, each containing custom certificates to trust.
     */
    constructor(keyStores: List<KeyStore>) {
        trustManagers = mutableListOf(getDefaultTrustManager())
        keyStores.map { trustManagers += getTrustManagerForKeyStore(it) }
    }

    /**
     * Retrieves the default system [X509TrustManager].
     *
     * This method initializes a [TrustManagerFactory] with a null [KeyStore], which uses the system’s default trust store.
     *
     * @return An [X509TrustManager] representing the system’s default trust manager.
     */
    private fun getDefaultTrustManager(): X509TrustManager = getTrustManagerForKeyStore(null)

    /**
     * Creates an [X509TrustManager] for a specific [KeyStore].
     *
     * @param keyStore The [KeyStore] to initialize the trust manager with, or `null` for the system default.
     * @return An [X509TrustManager] configured with the given [KeyStore].
     */
    private fun getTrustManagerForKeyStore(keyStore: KeyStore?): X509TrustManager {
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val factory = TrustManagerFactory.getInstance(algorithm)
        factory.init(keyStore)
        return factory.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    /**
     * Validates a client certificate chain against all trust managers.
     *
     * Iterates through the [trustManagers] list, attempting to validate the certificate chain. If any trust manager
     * succeeds, the method returns silently. If all fail, a [CertificateException] is thrown.
     *
     * @param chain The array of [X509Certificate] objects representing the client’s certificate chain.
     * @param authType The authentication type (e.g., "RSA", "DSA") used in the SSL handshake.
     * @throws CertificateException If no trust manager accepts the certificate chain.
     */
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
                // Ignored, continue to next trust manager
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    /**
     * Validates a server certificate chain against all trust managers.
     *
     * Iterates through the [trustManagers] list, attempting to validate the certificate chain. If any trust manager
     * succeeds, the method returns silently. If all fail, a [CertificateException] is thrown.
     *
     * @param chain The array of [X509Certificate] objects representing the server’s certificate chain.
     * @param authType The authentication type (e.g., "RSA", "DSA") used in the SSL handshake.
     * @throws CertificateException If no trust manager accepts the certificate chain.
     */
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
                // Ignored, continue to next trust manager
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    /**
     * Returns the combined list of accepted issuers from all trust managers.
     *
     * Aggregates the accepted issuers from each [trustManager] into a single array, avoiding duplicates by relying on
     * the natural iteration order.
     *
     * @return An array of [X509Certificate] objects representing all certificates trusted by the combined trust managers.
     */
    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        for (trustManager in trustManagers) {
            for (certificate in trustManager.acceptedIssuers) {
                certificates.add(certificate)
            }
        }
        return certificates.toTypedArray()
    }
}
