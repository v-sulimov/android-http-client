package com.vsulimov.httpclient

import com.vsulimov.httpclient.configuration.HttpClientConfiguration
import com.vsulimov.httpclient.exception.RedirectException
import com.vsulimov.httpclient.exception.UnsuccessfulResponseStatusCodeException
import com.vsulimov.httpclient.extensions.readTextAndClose
import com.vsulimov.httpclient.interceptor.RequestInterceptor
import com.vsulimov.httpclient.request.DeleteRequest
import com.vsulimov.httpclient.request.GetRequest
import com.vsulimov.httpclient.request.HeadRequest
import com.vsulimov.httpclient.request.OptionsRequest
import com.vsulimov.httpclient.request.PatchRequest
import com.vsulimov.httpclient.request.PostRequest
import com.vsulimov.httpclient.request.PutRequest
import com.vsulimov.httpclient.request.Request
import com.vsulimov.httpclient.request.RequestWithBody
import com.vsulimov.httpclient.response.Response
import com.vsulimov.httpclient.security.CompositeX509TrustManager
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * A configurable HTTP client for making requests to web servers.
 *
 * This client supports GET, POST, PUT, DELETE, HEAD, OPTIONS, and PATCH requests with features like
 * custom SSL certificates, request interceptors, configurable timeouts, and optional redirect
 * following. It provides a flexible and reusable way to interact with HTTP endpoints.
 *
 * @param configuration The configuration for this HTTP client. Defaults to a new [HttpClientConfiguration]
 * instance with default settings.
 */
class HttpClient(private val configuration: HttpClientConfiguration = HttpClientConfiguration.builder().build()) {

    private val requestInterceptors = mutableListOf<RequestInterceptor>()

    private val sslSocketFactory: SSLSocketFactory

    init {
        val keyStores = mutableListOf<KeyStore>()
        configuration.certificateInputStream?.let {
            keyStores += getKeyStoreForCertificateInputStream(it)
        }
        val sslContext = SSLContext.getInstance("TLS")
        val trustManagers = arrayOf(CompositeX509TrustManager(keyStores))
        sslContext.init(null, trustManagers, null)
        sslSocketFactory = sslContext.socketFactory
    }

    /**
     * Creates a [KeyStore] from the provided certificate input stream.
     *
     * This method generates a keystore containing an X.509 certificate, which is used to establish
     * trust for SSL connections.
     *
     * @param inputStream The input stream containing the X.509 certificate data.
     * @return A [KeyStore] instance containing the certificate.
     */
    private fun getKeyStoreForCertificateInputStream(inputStream: InputStream): KeyStore {
        val certificate = inputStream.use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }
    }

    /**
     * Adds a request interceptor to modify requests before they are sent.
     *
     * Interceptors can be used to add headers, log requests, or perform other pre-processing tasks.
     *
     * @param interceptor The [RequestInterceptor] to add to the list of interceptors.
     */
    fun addRequestInterceptor(interceptor: RequestInterceptor) {
        requestInterceptors.add(interceptor)
    }

    /**
     * Removes a specific request interceptor from the list.
     *
     * @param interceptor The [RequestInterceptor] to remove.
     */
    fun removeRequestInterceptor(interceptor: RequestInterceptor) {
        requestInterceptors.remove(interceptor)
    }

    /**
     * Clears all registered request interceptors.
     */
    fun removeAllRequestInterceptors() {
        requestInterceptors.clear()
    }

    /**
     * Executes a GET request to the specified URL.
     *
     * @param request The [GetRequest] containing the URL and headers.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executeGetRequest(request: GetRequest): Result<Response> = executeRequest(request)

    /**
     * Executes a POST request with a request body to the specified URL.
     *
     * @param request The [PostRequest] containing the URL, headers, and body.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executePostRequest(request: PostRequest): Result<Response> = executeRequest(request)

    /**
     * Executes a PUT request with a request body to the specified URL.
     *
     * @param request The [PutRequest] containing the URL, headers, and body.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executePutRequest(request: PutRequest): Result<Response> = executeRequest(request)

    /**
     * Executes a DELETE request to the specified URL.
     *
     * @param request The [DeleteRequest] containing the URL and headers.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executeDeleteRequest(request: DeleteRequest): Result<Response> = executeRequest(request)

    /**
     * Executes a HEAD request to the specified URL.
     *
     * @param request The [HeadRequest] containing the URL and headers.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executeHeadRequest(request: HeadRequest): Result<Response> = executeRequest(request)

    /**
     * Executes an OPTIONS request to the specified URL.
     *
     * @param request The [OptionsRequest] containing the URL and headers.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executeOptionsRequest(request: OptionsRequest): Result<Response> = executeRequest(request)

    /**
     * Executes a PATCH request with a request body to the specified URL.
     *
     * @param request The [PatchRequest] containing the URL, headers, and body.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    fun executePatchRequest(request: PatchRequest): Result<Response> = executeRequest(request)

    /**
     * Executes an HTTP request and handles the response.
     *
     * This method performs the following steps:
     * 1. Applies registered interceptors to the request.
     * 2. Sets up the HTTP connection with timeouts, SSL (if applicable), and headers.
     * 3. Sends the request, including the body for [RequestWithBody] instances.
     * 4. Processes the response, handling success, redirects, or errors.
     *
     * @param request The [Request] to execute (e.g., [GetRequest], [PostRequest]).
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    private fun executeRequest(request: Request): Result<Response> {
        var connection: HttpURLConnection? = null
        applyInterceptors(request)
        val url = URL(request.url)
        try {
            connection = url.openConnection() as HttpURLConnection
            if (connection is HttpsURLConnection) {
                connection.sslSocketFactory = sslSocketFactory
            }
            connection.run {
                readTimeout = configuration.readTimeout
                connectTimeout = configuration.connectTimeout
                requestMethod = request.requestMethod.toString()
                doInput = true
                setRequestProperty("Accept", "application/json")
                request.headers.forEach { addRequestProperty(it.name, it.value) }

                // Handle request body for POST/PUT/PATCH
                if (request is RequestWithBody) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    val requestBodyByteArray = request.body.toByteArray()
                    outputStream.write(requestBodyByteArray)
                }

                connect()
                val responseCode = responseCode

                if (isRequestSuccessful(responseCode)) {
                    val responseBody = inputStream.readTextAndClose()
                    val headers = headerFields.mapValues { it.value.joinToString(",") }
                    val response = Response(responseCode, responseBody, headers)
                    return Result.success(response)
                } else if (isRedirect(responseCode) && configuration.followRedirects) {
                    val location = getHeaderField("Location")
                    if (location != null) {
                        val redirectRequest = GetRequest(location)
                        redirectRequest.headers.addAll(request.headers)
                        return executeRequest(redirectRequest) // Follow redirect
                    } else {
                        return Result.failure(RedirectException(request.url, responseCode, "No Location header"))
                    }
                } else {
                    val responseBody = if (isRedirect(responseCode)) {
                        inputStream.readTextAndClose()
                    } else {
                        errorStream?.readTextAndClose().orEmpty()
                    }
                    val exception = if (isRedirect(responseCode)) {
                        RedirectException(request.url, responseCode, responseBody)
                    } else {
                        UnsuccessfulResponseStatusCodeException(
                            requestUrl = request.url,
                            responseCode = responseCode,
                            errorBody = responseBody
                        )
                    }
                    return Result.failure(exception)
                }
            }
        } catch (exception: IOException) {
            return Result.failure(exception)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Applies all registered request interceptors to the given request.
     *
     * Interceptors are executed in the order they were added, allowing sequential modification of
     * the request (e.g., adding authentication headers).
     *
     * @param request The [Request] to modify with interceptors.
     */
    private fun applyInterceptors(request: Request) {
        for (interceptor in requestInterceptors) {
            interceptor.intercept(request)
        }
    }

    /**
     * Determines if the HTTP response code indicates a successful request.
     *
     * @param responseCode The HTTP status code from the server.
     * @return `true` if the code is in the range 200-299 (inclusive), `false` otherwise.
     */
    private fun isRequestSuccessful(responseCode: Int): Boolean = responseCode in 200..299

    /**
     * Determines if the HTTP response code indicates a redirect.
     *
     * @param responseCode The HTTP status code from the server.
     * @return `true` if the code is in the range 300-399 (inclusive), `false` otherwise.
     */
    private fun isRedirect(responseCode: Int): Boolean = responseCode in 300..399
}
