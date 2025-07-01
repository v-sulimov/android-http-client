package com.vsulimov.httpclient.response

import java.io.InputStream

/**
 * Represents an HTTP response whose body is delivered as a raw [InputStream].
 *
 * Unlike [Response], which buffers the entire response body into a [String], [StreamingResponse]
 * exposes the body incrementally. This is suited for large downloads, binary payloads, or any case
 * where loading the full body into memory would be wasteful or impractical.
 *
 * ### Lifecycle
 * A [StreamingResponse] is valid **only for the duration of the `streamHandler` lambda** passed to
 * the corresponding `executeStreamingXxxRequest` method. The underlying HTTP connection is kept open
 * while the lambda executes and is disconnected immediately after it returns. **Do not retain a
 * reference to [body] beyond the scope of the handler.**
 *
 * ### Usage
 * ```kotlin
 * val client = HttpClient()
 * val result = client.executeStreamingGetRequest(GetRequest("https://example.com/large-file.bin")) { response ->
 *     FileOutputStream("large-file.bin").use { response.body.copyTo(it) }
 * }
 * ```
 *
 * @param statusCode The HTTP status code returned by the server (e.g., 200 for OK).
 * @param headers A case-insensitive [Map] of HTTP response header names to their values. Multi-value
 * headers are joined with `", "`. The HTTP status-line pseudo-header is excluded.
 * @param body The response body as a raw [InputStream]. Read or transfer this stream inside the
 * handler lambda; it becomes invalid once the handler returns.
 */
class StreamingResponse(val statusCode: Int, val headers: Map<String, String>, val body: InputStream)
