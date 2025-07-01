package com.vsulimov.httpclient.exception

/**
 * Exception thrown when an HTTP request fails due to an unsuccessful status code from the server.
 *
 * This exception is triggered when the HTTP client receives a response with a status code outside the successful range
 * of 200-299, indicating that the request could not be completed as expected. Such status codes typically fall into the
 * 400-599 range, covering client errors (400-499) and server errors (500-599). The exception provides detailed information
 * about the failure, including the request URL, the error status code, and the error response body, which may include
 * server-provided error messages or diagnostic data.
 *
 * ### Common Use Cases
 * - Handling client-side errors (e.g., 404 Not Found, 400 Bad Request) in an application.
 * - Debugging server-side issues (e.g., 500 Internal Server Error) by inspecting the `errorBody`.
 * - Logging or reporting HTTP failures for monitoring purposes.
 *
 * ### Example
 * ```kotlin
 * try {
 *     // HTTP request logic here
 * } catch (e: UnsuccessfulResponseStatusCodeException) {
 *     println("Request failed: ${e.message}")
 *     println("Error body: ${e.errorBody}")
 * }
 * ```
 *
 * @param requestUrl The original URL of the HTTP request that failed, represented as a `String`
 *                   (e.g., "https://example.com/api/resource").
 * @param responseCode The HTTP status code returned by the server, typically in the range 400-599.
 *                     Common values include:
 *                     - 400 (Bad Request)
 *                     - 404 (Not Found)
 *                     - 500 (Internal Server Error)
 *                     - 503 (Service Unavailable)
 * @param errorBody The body of the error response as a `String`. This field often contains server-generated error messages,
 *                  JSON payloads, or other diagnostic information that can help identify the cause of the failure.
 *                  Developers should examine this property for actionable details.
 */
class UnsuccessfulResponseStatusCodeException(requestUrl: String, responseCode: Int, val errorBody: String) :
    Exception() {

    /**
     * A detailed message describing the unsuccessful HTTP response.
     *
     * This property generates a human-readable string that includes the original request URL and the HTTP status code
     * indicating the failure. It also suggests checking the `errorBody` property for additional context, such as error
     * descriptions or server logs provided in the response.
     *
     * ### Format
     * "Request to [requestUrl] failed with status code [responseCode]. See exception errorBody for details."
     *
     * ### Example Output
     * "Request to https://example.com/api/resource failed with status code 404. See exception errorBody for details."
     */
    override val message =
        "Request to $requestUrl failed with status code $responseCode. " +
            "See exception errorBody for details."
}
