package com.vsulimov.httpclient.exception

/**
 * Exception thrown when an HTTP request encounters a redirect response from the server.
 *
 * This exception is raised by the HTTP client when it receives a response with an HTTP status code in the 300-399 range,
 * indicating that the requested resource has been redirected to another location. It encapsulates details about
 * the redirect, including the original request URL, the redirect status code, and the raw HTTP response body
 * (typically an HTML "Moved Permanently" page). The redirect target URL is not included here; it was in the
 * `Location` response header that triggered this exception.
 *
 * ### Common Use Cases
 * - Identifying when a server redirects a request unexpectedly (e.g., when [HttpClientConfiguration.followRedirects] is `false`).
 * - Logging or reporting redirect events in an application.
 * - Detecting redirect loops or missing `Location` headers via [responseBody].
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
 * @param responseBody The raw HTTP response body as a `String`. For redirect responses this is typically a short HTML
 *                     page (e.g., "301 Moved Permanently") or empty. It does not contain the redirect target URL,
 *                     which was in the `Location` header.
 */
class RedirectException(requestUrl: String, val responseCode: Int, val responseBody: String) : Exception() {

    /**
     * A detailed message describing the redirect event.
     *
     * This property constructs a human-readable string that includes the original request URL and the HTTP status code
     * of the redirect response. It advises the developer to check the `responseBody` property for the raw HTTP
     * response body (typically a brief HTML page).
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
