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
import com.vsulimov.httpclient.request.StreamingPatchRequest
import com.vsulimov.httpclient.request.StreamingPostRequest
import com.vsulimov.httpclient.request.StreamingPutRequest
import com.vsulimov.httpclient.request.StreamingRequestWithBody
import com.vsulimov.httpclient.response.Response
import com.vsulimov.httpclient.response.StreamingResponse
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
 * Streaming variants (`executeStreaming...Request`) are available for large downloads and uploads.
 * They expose the response body as a raw [java.io.InputStream] via a scoped handler lambda and
 * transmit upload bodies using chunked transfer encoding, avoiding full in-memory buffering.
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
        sslContext.init(null, arrayOf(CompositeX509TrustManager(keyStores)), null)
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
     * Executes a GET request and delivers the response body as a raw [InputStream] to [streamHandler].
     *
     * The [StreamingResponse] passed to [streamHandler] is valid only for the duration of the lambda.
     * The underlying connection is disconnected immediately after the lambda returns. Do not retain
     * a reference to [StreamingResponse.body] beyond the scope of the handler.
     *
     * @param request The [GetRequest] containing the URL and headers.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    fun <T> executeStreamingGetRequest(request: GetRequest, streamHandler: (StreamingResponse) -> T): Result<T> =
        executeStreamingRequest(request, streamHandler)

    /**
     * Executes a POST request with a streaming body and delivers the response body as a raw [InputStream]
     * to [streamHandler].
     *
     * The request body is read from [StreamingPostRequest.bodyStream] and transmitted using chunked
     * transfer encoding. The [StreamingResponse] passed to [streamHandler] is valid only for the
     * duration of the lambda; the connection is disconnected immediately after the lambda returns.
     *
     * @param request The [StreamingPostRequest] containing the URL, headers, and body stream.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    fun <T> executeStreamingPostRequest(
        request: StreamingPostRequest,
        streamHandler: (StreamingResponse) -> T
    ): Result<T> = executeStreamingRequest(request, streamHandler)

    /**
     * Executes a PUT request with a streaming body and delivers the response body as a raw [InputStream]
     * to [streamHandler].
     *
     * The request body is read from [StreamingPutRequest.bodyStream] and transmitted using chunked
     * transfer encoding. The [StreamingResponse] passed to [streamHandler] is valid only for the
     * duration of the lambda; the connection is disconnected immediately after the lambda returns.
     *
     * @param request The [StreamingPutRequest] containing the URL, headers, and body stream.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    fun <T> executeStreamingPutRequest(
        request: StreamingPutRequest,
        streamHandler: (StreamingResponse) -> T
    ): Result<T> = executeStreamingRequest(request, streamHandler)

    /**
     * Executes a PATCH request with a streaming body and delivers the response body as a raw [InputStream]
     * to [streamHandler].
     *
     * The request body is read from [StreamingPatchRequest.bodyStream] and transmitted using chunked
     * transfer encoding. The [StreamingResponse] passed to [streamHandler] is valid only for the
     * duration of the lambda; the connection is disconnected immediately after the lambda returns.
     *
     * @param request The [StreamingPatchRequest] containing the URL, headers, and body stream.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    fun <T> executeStreamingPatchRequest(
        request: StreamingPatchRequest,
        streamHandler: (StreamingResponse) -> T
    ): Result<T> = executeStreamingRequest(request, streamHandler)

    /**
     * Applies registered interceptors to the request, then delegates to [performRequest].
     *
     * Separating interceptor application from the HTTP call ensures interceptors run exactly once
     * per user-initiated request and are not re-applied when [performRequest] calls itself
     * recursively to follow a redirect.
     *
     * @param request The [Request] to execute.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    private fun executeRequest(request: Request): Result<Response> {
        applyInterceptors(request)
        return performRequest(request)
    }

    /**
     * Opens an HTTP connection, sends the request, and processes the response.
     *
     * Redirects are followed recursively (up to [HttpClientConfiguration.maxRedirects] hops) without
     * re-applying interceptors — the already-intercepted headers from the original request are
     * forwarded to the redirect target.
     *
     * @param request The [Request] to send.
     * @param redirectDepth The current redirect depth; used to cap redirect chains.
     * @return A [Result] containing either a [Response] on success or an exception on failure.
     */
    private fun performRequest(request: Request, redirectDepth: Int = 0): Result<Response> {
        if (redirectDepth > configuration.maxRedirects) {
            return Result.failure(
                RedirectException(request.url, 0, "Too many redirects (max: ${configuration.maxRedirects})")
            )
        }
        var connection: HttpURLConnection? = null
        try {
            val conn = URL(request.url).openConnection() as HttpURLConnection
            connection = conn
            if (conn is HttpsURLConnection) {
                conn.sslSocketFactory = sslSocketFactory
            }
            conn.instanceFollowRedirects = false
            conn.readTimeout = configuration.readTimeout
            conn.connectTimeout = configuration.connectTimeout
            conn.requestMethod = request.requestMethod.toString()
            request.headers.forEach { conn.addRequestProperty(it.name, it.value) }
            if (request is RequestWithBody) {
                conn.doOutput = true
                conn.outputStream.use { it.write(request.body.toByteArray()) }
            }
            val responseCode = conn.responseCode
            return when {
                isRequestSuccessful(responseCode) -> {
                    val responseBody = conn.inputStream.readTextAndClose()
                    val headers = conn.headerFields.entries
                        .mapNotNull { (key, value) -> key?.let { it to value.joinToString(",") } }
                        .toMap()
                    Result.success(Response(responseCode, responseBody, headers))
                }

                isRedirect(responseCode) && configuration.followRedirects -> {
                    val location = conn.getHeaderField("Location")
                    if (location != null) {
                        val redirectRequest = GetRequest(location)
                        redirectRequest.headers.addAll(request.headers)
                        performRequest(redirectRequest, redirectDepth + 1)
                    } else {
                        Result.failure(RedirectException(request.url, responseCode, "No Location header"))
                    }
                }

                isRedirect(responseCode) -> {
                    val responseBody = conn.inputStream.readTextAndClose()
                    Result.failure(RedirectException(request.url, responseCode, responseBody))
                }

                else -> {
                    val responseBody = conn.errorStream?.readTextAndClose().orEmpty()
                    Result.failure(
                        UnsuccessfulResponseStatusCodeException(
                            requestUrl = request.url,
                            responseCode = responseCode,
                            errorBody = responseBody
                        )
                    )
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
        requestInterceptors.forEach { it.intercept(request) }
    }

    /**
     * Applies registered interceptors to the request, then delegates to [performStreamingRequest].
     *
     * @param request The [Request] to execute with a streaming response.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    private fun <T> executeStreamingRequest(request: Request, streamHandler: (StreamingResponse) -> T): Result<T> {
        applyInterceptors(request)
        return performStreamingRequest(request, streamHandler)
    }

    /**
     * Opens an HTTP connection, sends the request, and invokes [streamHandler] with a [StreamingResponse]
     * that wraps the raw response [java.io.InputStream].
     *
     * Unlike [performRequest], this method does not buffer the response body into a [String]. Instead it
     * passes the live [java.io.InputStream] to [streamHandler] and disconnects the connection only after
     * the lambda returns, keeping the stream valid for the duration of the handler.
     *
     * Redirect following is not performed; a redirect response is surfaced as a [RedirectException] failure.
     *
     * @param request The [Request] to send.
     * @param streamHandler A lambda that receives the [StreamingResponse] and returns a value of type [T].
     * @return A [Result] containing the value returned by [streamHandler] on success, or an exception on failure.
     */
    private fun <T> performStreamingRequest(request: Request, streamHandler: (StreamingResponse) -> T): Result<T> {
        var connection: HttpURLConnection? = null
        try {
            val conn = URL(request.url).openConnection() as HttpURLConnection
            connection = conn
            if (conn is HttpsURLConnection) {
                conn.sslSocketFactory = sslSocketFactory
            }
            conn.instanceFollowRedirects = false
            conn.readTimeout = configuration.readTimeout
            conn.connectTimeout = configuration.connectTimeout
            conn.requestMethod = request.requestMethod.toString()
            request.headers.forEach { conn.addRequestProperty(it.name, it.value) }
            if (request is StreamingRequestWithBody) {
                conn.doOutput = true
                conn.setChunkedStreamingMode(0)
                conn.outputStream.use { out -> request.bodyStream.use { it.copyTo(out) } }
            }
            val responseCode = conn.responseCode
            return when {
                isRequestSuccessful(responseCode) -> {
                    val headers = conn.headerFields.entries
                        .mapNotNull { (key, value) -> key?.let { it to value.joinToString(",") } }
                        .toMap()
                    Result.success(streamHandler(StreamingResponse(responseCode, headers, conn.inputStream)))
                }

                isRedirect(responseCode) -> {
                    val responseBody = conn.errorStream?.readTextAndClose().orEmpty()
                    Result.failure(RedirectException(request.url, responseCode, responseBody))
                }

                else -> {
                    val responseBody = conn.errorStream?.readTextAndClose().orEmpty()
                    Result.failure(
                        UnsuccessfulResponseStatusCodeException(
                            requestUrl = request.url,
                            responseCode = responseCode,
                            errorBody = responseBody
                        )
                    )
                }
            }
        } catch (exception: IOException) {
            return Result.failure(exception)
        } finally {
            connection?.disconnect()
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
