package com.vsulimov.httpclient.exception

/**
 * Exception thrown when an HTTP request encounters a redirect response from the server.
 *
 * This exception is raised by the HTTP client when it receives a response with an HTTP status code in the 300-399 range,
 * indicating that the requested resource has been redirected to another location. It encapsulates critical details about
 * the redirect, including the original request URL, the redirect status code, and the response body, which may contain
 * additional information such as the new location (e.g., in the `Location` header or body). This exception is useful for
 * debugging redirect issues or programmatically handling redirect behavior in an HTTP client.
 *
 * ### Common Use Cases
 * - Identifying when a server redirects a request unexpectedly.
 * - Extracting redirect details (e.g., new URL) from the `responseBody` for further processing.
 * - Logging or reporting redirect events in an application.
 *
 * ### Example
 * ```kotlin
 * try {
 *     // HTTP request logic here
 * } catch (e: RedirectException) {
 *     println("Redirect occurred: ${e.message}")
 *     println("Response body: ${e.responseBody}")
 * }
 * ```
 *
 * @param requestUrl The original URL targeted by the HTTP request that resulted in a redirect.
 *                   This is a `String` representing the full URL (e.g., "https://example.com/api/resource").
 * @param responseCode The HTTP status code returned by the server, typically in the range 300-399.
 *                     Common values include:
 *                     - 301 (Moved Permanently)
 *                     - 302 (Found)
 *                     - 307 (Temporary Redirect)
 *                     - 308 (Permanent Redirect)
 * @param responseBody The body of the redirect response as a `String`. This may contain additional details about the redirect,
 *                     such as a human-readable message or the new location if provided by the server. Developers should
 *                     inspect this field for further context about the redirect.
 */
class RedirectException(requestUrl: String, responseCode: Int, val responseBody: String) : Exception() {

    /**
     * A detailed message describing the redirect event.
     *
     * This property constructs a human-readable string that includes the original request URL and the HTTP status code
     * of the redirect response. It also advises the developer to check the `responseBody` property for additional details,
     * such as the new resource location or server-specific redirect information.
     *
     * ### Format
     * "Request to [requestUrl] was redirected (status code [responseCode]). See exception responseBody for details."
     *
     * ### Example Output
     * "Request to https://example.com/api/resource was redirected (status code 301). See exception responseBody for details."
     */
    override val message =
        "Request to $requestUrl was redirected (status code $responseCode). " +
            "See exception responseBody for details."
}
