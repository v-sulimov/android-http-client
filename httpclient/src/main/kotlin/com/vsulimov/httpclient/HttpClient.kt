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
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.TreeMap
import java.util.concurrent.CopyOnWriteArrayList
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
 * ### Thread Safety
 * This class is thread-safe. A single [HttpClient] instance may be shared across threads and used
 * to execute concurrent requests. The [SSLSocketFactory] is created once during construction and
 * never mutated. Interceptor list modifications ([addRequestInterceptor], [removeRequestInterceptor],
 * [removeAllRequestInterceptors]) are safe to call from any thread at any time.
 *
 * @param configuration The configuration for this HTTP client. Defaults to a new [HttpClientConfiguration]
 * instance with default settings.
 */
class HttpClient(private val configuration: HttpClientConfiguration = HttpClientConfiguration.builder().build()) {

    private val requestInterceptors = CopyOnWriteArrayList<RequestInterceptor>()

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
     * re-applying interceptors. On same-origin redirects all headers are forwarded; on cross-origin
     * redirects (different scheme, host, or port) sensitive headers (Authorization, Cookie, etc.)
     * are stripped.
     *
     * HTTP 304 Not Modified is treated as a successful response with an empty body, allowing callers
     * that send conditional requests (If-None-Match / If-Modified-Since) to observe the 304 status
     * code and its headers directly.
     *
     * The response body charset is detected from the `Content-Type` header (e.g.,
     * `text/html; charset=ISO-8859-1`). If no charset is specified, UTF-8 is used.
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
                val bodyBytes = request.body.toByteArray(Charsets.UTF_8)
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(bodyBytes.size.toLong())
                conn.outputStream.use { it.write(bodyBytes) }
            }
            val responseCode = conn.responseCode
            return when {
                isRequestSuccessful(responseCode) -> {
                    val headers = buildHeaders(conn)
                    val responseBody = conn.inputStream.readTextAndClose(extractCharset(headers["Content-Type"]))
                    Result.success(Response(responseCode, responseBody, headers))
                }

                responseCode == HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    Result.success(Response(responseCode, "", buildHeaders(conn)))
                }

                isRedirect(responseCode) && configuration.followRedirects -> {
                    val location = conn.getHeaderField("Location")
                    if (location != null) {
                        val resolvedLocation = runCatching { URL(URL(request.url), location).toString() }.getOrDefault(
                            location
                        )
                        val redirectRequest = createRedirectRequest(resolvedLocation, request)
                        conn.disconnect()
                        connection = null
                        performRequest(redirectRequest, redirectDepth + 1)
                    } else {
                        Result.failure(RedirectException(request.url, responseCode, "No Location header"))
                    }
                }

                isRedirect(responseCode) -> {
                    val responseBody = try { conn.inputStream.readTextAndClose() } catch (e: IOException) { "" }
                    Result.failure(RedirectException(request.url, responseCode, responseBody))
                }

                else -> {
                    val responseBody = try {
                        (conn.errorStream ?: conn.inputStream)?.readTextAndClose().orEmpty()
                    } catch (e: IOException) {
                        ""
                    }
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
     * Creates a [GetRequest] for a redirect target, copying safe headers from [originalRequest].
     *
     * Headers in [SENSITIVE_HEADERS] are stripped when the redirect crosses to a different origin
     * (different scheme, host, or port), preventing authentication tokens and cookies from being
     * leaked to untrusted origins. Header names are compared case-insensitively per RFC 7230.
     *
     * @param location The resolved absolute URL of the redirect target.
     * @param originalRequest The original request whose headers are candidates for forwarding.
     * @return A [GetRequest] configured for the redirect target with appropriate headers.
     */
    private fun createRedirectRequest(location: String, originalRequest: Request): GetRequest {
        val redirectRequest = GetRequest(location)
        val headersToForward = if (isSameOrigin(originalRequest.url, location)) {
            originalRequest.headers
        } else {
            originalRequest.headers.filter { it.name.lowercase() !in SENSITIVE_HEADERS }
        }
        redirectRequest.headers.addAll(headersToForward)
        return redirectRequest
    }

    /**
     * Returns `true` if [url1] and [url2] share the same origin (scheme, host, and port).
     *
     * The comparison prevents sensitive headers from being forwarded on scheme downgrades
     * (e.g., HTTPS to HTTP) or cross-port redirects, not just cross-host redirects.
     */
    private fun isSameOrigin(url1: String, url2: String): Boolean {
        val u1 = runCatching { URL(url1) }.getOrNull() ?: return false
        val u2 = runCatching { URL(url2) }.getOrNull() ?: return false
        val port1 = if (u1.port == -1) u1.defaultPort else u1.port
        val port2 = if (u2.port == -1) u2.defaultPort else u2.port
        return u1.protocol.equals(u2.protocol, ignoreCase = true) &&
            u1.host.equals(u2.host, ignoreCase = true) &&
            port1 == port2
    }

    /**
     * Builds a case-insensitive response header map from the connection's header fields.
     *
     * The null key present in [HttpURLConnection.getHeaderFields] (representing the HTTP status line)
     * is filtered out. Multiple values for a single header name are joined with `", "` per RFC 7230.
     *
     * @param conn The [HttpURLConnection] after the response has been received.
     * @return A case-insensitive [Map] of header names to their values.
     */
    private fun buildHeaders(conn: HttpURLConnection): Map<String, String> =
        conn.headerFields.entries
            .mapNotNull { (key, value) -> key?.let { it to value.joinToString(", ") } }
            .toMap(TreeMap(String.CASE_INSENSITIVE_ORDER))

    /**
     * Extracts the charset from a `Content-Type` header value.
     *
     * Parses values of the form `"text/html; charset=ISO-8859-1"` and returns the named [Charset].
     * Quoted charset names (e.g., `charset="UTF-8"`) are handled correctly. Falls back to UTF-8 if
     * no charset parameter is present or the named charset is not supported by the current JVM.
     *
     * @param contentType The raw `Content-Type` header value, or `null` if the header is absent.
     * @return The [Charset] to use for decoding the response body.
     */
    private fun extractCharset(contentType: String?): Charset {
        if (contentType != null) {
            for (part in contentType.split(';')) {
                val trimmed = part.trim()
                if (trimmed.startsWith("charset=", ignoreCase = true)) {
                    val name = trimmed.substring(8).trim().removeSurrounding("\"")
                    runCatching { Charset.forName(name) }.getOrNull()?.let { return it }
                }
            }
        }
        return Charsets.UTF_8
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
                    Result.success(streamHandler(StreamingResponse(responseCode, buildHeaders(conn), conn.inputStream)))
                }

                responseCode == HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    Result.success(
                        streamHandler(
                            StreamingResponse(responseCode, buildHeaders(conn), ByteArrayInputStream(ByteArray(0)))
                        )
                    )
                }

                isRedirect(responseCode) -> {
                    val responseBody = try { conn.inputStream.readTextAndClose() } catch (e: IOException) { "" }
                    Result.failure(RedirectException(request.url, responseCode, responseBody))
                }

                else -> {
                    val responseBody = try {
                        (conn.errorStream ?: conn.inputStream)?.readTextAndClose().orEmpty()
                    } catch (_: IOException) {
                        ""
                    }
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

    companion object {
        /**
         * HTTP header names (lowercase) that must not be forwarded to a different host on redirect.
         * Compared case-insensitively so both "Authorization" and "authorization" are matched.
         */
        private val SENSITIVE_HEADERS = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "proxy-authenticate",
            "www-authenticate"
        )
    }
}
