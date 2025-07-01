package com.vsulimov.httpclient.request

import java.io.InputStream

/**
 * An abstract base class for HTTP requests that send their body as an [InputStream].
 *
 * [StreamingRequestWithBody] is the streaming counterpart of [RequestWithBody]. Where
 * [RequestWithBody] holds the entire payload in memory as a [String], this class reads the body
 * from an [InputStream], making it suited for large uploads — such as files or binary data — where
 * buffering the full content in memory would be wasteful.
 *
 * When the request is executed, the HTTP client transmits the body using chunked transfer encoding,
 * since the total payload size is not known in advance.
 *
 * ### Properties
 * - **bodyStream**: The [InputStream] providing the request payload. The client reads and closes
 *   it during execution; do not reuse it after the request has been sent.
 * - Inherits `headers`, `requestMethod`, and `url` from [Request].
 *
 * @param requestMethod The HTTP method for the request (e.g., POST, PUT).
 * @param url The target URL of the request.
 * @param bodyStream The [InputStream] providing the request payload.
 */
abstract class StreamingRequestWithBody(requestMethod: RequestMethod, url: String, val bodyStream: InputStream) :
    Request(requestMethod = requestMethod, url = url)

/**
 * Represents an HTTP POST request whose body is supplied as an [InputStream].
 *
 * [StreamingPostRequest] is the streaming counterpart of [PostRequest], suited for large uploads
 * such as files or binary data where buffering the entire payload in memory as a [String] would be
 * impractical. The body is transmitted using chunked transfer encoding.
 *
 * Set a `Content-Type` header on the request to inform the server how to interpret the payload.
 *
 * **Example:**
 * ```kotlin
 * val request = StreamingPostRequest(
 *     url = "https://api.example.com/upload",
 *     bodyStream = File("upload.bin").inputStream()
 * )
 * request.headers.add(Header("Content-Type", "application/octet-stream"))
 * ```
 *
 * @param url The target URL for the POST request.
 * @param bodyStream The [InputStream] providing the request payload.
 */
class StreamingPostRequest(url: String, bodyStream: InputStream) :
    StreamingRequestWithBody(requestMethod = RequestMethod.POST, url = url, bodyStream = bodyStream)

/**
 * Represents an HTTP PUT request whose body is supplied as an [InputStream].
 *
 * [StreamingPutRequest] is the streaming counterpart of [PutRequest], suited for large uploads
 * such as files or binary data where buffering the entire payload in memory as a [String] would be
 * impractical. The body is transmitted using chunked transfer encoding.
 *
 * Set a `Content-Type` header on the request to inform the server how to interpret the payload.
 *
 * **Example:**
 * ```kotlin
 * val request = StreamingPutRequest(
 *     url = "https://api.example.com/files/1",
 *     bodyStream = File("upload.bin").inputStream()
 * )
 * request.headers.add(Header("Content-Type", "application/octet-stream"))
 * ```
 *
 * @param url The target URL for the PUT request.
 * @param bodyStream The [InputStream] providing the request payload.
 */
class StreamingPutRequest(url: String, bodyStream: InputStream) :
    StreamingRequestWithBody(requestMethod = RequestMethod.PUT, url = url, bodyStream = bodyStream)

/**
 * Represents an HTTP PATCH request whose body is supplied as an [InputStream].
 *
 * [StreamingPatchRequest] is the streaming counterpart of [PatchRequest], suited for sending
 * partial updates from a data stream where buffering the full payload in memory as a [String] would
 * be impractical. The body is transmitted using chunked transfer encoding.
 *
 * Set a `Content-Type` header on the request to inform the server how to interpret the payload.
 *
 * **Example:**
 * ```kotlin
 * val request = StreamingPatchRequest(
 *     url = "https://api.example.com/files/1",
 *     bodyStream = File("patch.bin").inputStream()
 * )
 * request.headers.add(Header("Content-Type", "application/octet-stream"))
 * ```
 *
 * @param url The target URL for the PATCH request.
 * @param bodyStream The [InputStream] providing the request payload.
 */
class StreamingPatchRequest(url: String, bodyStream: InputStream) :
    StreamingRequestWithBody(requestMethod = RequestMethod.PATCH, url = url, bodyStream = bodyStream)
