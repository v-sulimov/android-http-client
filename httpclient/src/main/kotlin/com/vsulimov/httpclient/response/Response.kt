package com.vsulimov.httpclient.response

/**
 * Represents an HTTP response received from a server, including status code, body, and headers.
 *
 * The [Response] class encapsulates the full details of an HTTP response returned after executing a request via an HTTP
 * client. It includes the status code indicating the outcome of the request, the response body containing the server's
 * data, and a map of response headers providing metadata about the response. This class is immutable, ensuring that once
 * a response is created, its properties cannot be modified.
 *
 * ### Purpose
 * - To provide a comprehensive structure for accessing the results of an HTTP request.
 * - To serve as the success result type in the HTTP client's callback (e.g., `Result.success(Response)`).
 * - To expose response headers for additional processing or inspection (e.g., content type, caching directives).
 *
 * ### Properties
 * - **statusCode**: The HTTP status code (e.g., 200 for OK, 404 for Not Found) indicating the result of the request.
 * - **body**: The response body as a [String], typically containing data like JSON, HTML, or plain text returned by the server.
 * - **headers**: A map of HTTP headers where each key is a header name (e.g., "Content-Type") and each value is the
 *   corresponding header value (e.g., "application/json"). Multiple values for a single header are joined into a
 *   comma-separated string.
 *
 * ### Usage
 * This class is instantiated by the HTTP client when a request completes successfully (status code 200-299). Developers
 * can access its properties to process the response data, check the status, or inspect headers.
 *
 * **Example:**
 * ```kotlin
 * val client = HttpClient()
 * client.executeGetRequest(GetRequest("https://api.example.com")) { result ->
 *     result.onSuccess { response ->
 *         println("Status: ${response.statusCode}") // e.g., 200
 *         println("Body: ${response.body}") // e.g., {"message": "Hello"}
 *         println("Content-Type: ${response.headers["Content-Type"]}") // e.g., "application/json"
 *     }.onFailure { exception ->
 *         println("Request failed: $exception")
 *     }
 * }
 * ```
 *
 * ### Notes
 * - The `body` is a [String], assuming the HTTP client decodes the response bytes using a charset (e.g., UTF-8).
 *   For binary data, additional handling may be required outside this class.
 * - The `headers` map contains header names as keys (case-insensitive per HTTP standards) and their values as strings.
 *   If a header has multiple values (e.g., multiple "Set-Cookie" headers), they are combined into a single string with
 *   commas (e.g., "value1, value2").
 * - Header names may be `null` in the raw `headerFields` map from [HttpURLConnection] (representing the status line),
 *   but such entries are typically filtered out or handled by the HTTP client before constructing this object.
 *
 * @param statusCode The HTTP status code returned by the server (e.g., 200, 404, 500). This is an integer representing
 *                   the result of the request per HTTP standards (RFC 7231).
 * @param body The response body as a [String], containing the data returned by the server. This may be empty if the server
 *             sends no content (e.g., for a 204 No Content response).
 * @param headers A [Map] of HTTP header names to their values, where each value is a [String]. For headers with multiple
 *                values, the values are joined with commas (e.g., "value1, value2").
 */
class Response(val statusCode: Int, val body: String, val headers: Map<String, String>)
