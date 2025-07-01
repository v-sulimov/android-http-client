package com.vsulimov.httpclient.interceptor

import com.vsulimov.httpclient.request.Request

/**
 * An abstract base class for intercepting and modifying HTTP requests before they are executed.
 *
 * The [RequestInterceptor] class defines a contract for intercepting [Request] objects in an HTTP client.
 * Interceptors are useful for performing pre-processing tasks on requests, such as adding headers,
 * modifying the request URL, logging request details, or enforcing authentication policies. This
 * class is part of an interceptor chain pattern, where multiple interceptors can be applied sequentially
 * to a request before it is sent to the server.
 *
 * ### Purpose
 * - **Extensibility**: Allows developers to customize request behavior without altering the core HTTP client logic.
 * - **Modularity**: Separates cross-cutting concerns (e.g., logging, authentication) from the request execution.
 * - **Chainability**: Supports multiple interceptors that can be executed in order, enabling layered request processing.
 *
 * ### Usage
 * To use this class, create a concrete subclass and override the [intercept] method with your custom logic.
 * Instances of the subclass can then be added to an HTTP client's interceptor chain via a method like
 * `addRequestInterceptor`. The HTTP client will invoke each interceptor's [intercept] method before sending
 * the request.
 *
 * ### Example
 * ```kotlin
 * // Define a custom interceptor to add an Authorization header
 * class AuthInterceptor(private val token: String) : RequestInterceptor() {
 *     override fun intercept(request: Request) {
 *         request.headers.add(Header("Authorization", "Bearer $token"))
 *     }
 * }
 *
 * // Usage in an HTTP client
 * val client = HttpClient()
 * client.addRequestInterceptor(AuthInterceptor("my-token"))
 * client.executeGetRequest(GetRequest("https://api.example.com")) { result ->
 *     // Handle result
 * }
 * ```
 *
 * ### Implementation Guidelines
 * - **Thread Safety**: Ensure your interceptor implementation is thread-safe if used in a multi-threaded environment,
 *   as the same instance may be invoked concurrently by multiple requests.
 * - **Performance**: Keep the [intercept] method lightweight to avoid introducing significant latency to request processing.
 * - **Modification**: The [Request] object passed to [intercept] is mutable; modify it directly to apply changes (e.g., adding headers).
 * - **Error Handling**: Avoid throwing exceptions in [intercept] unless absolutely necessary, as they may disrupt the request pipeline.
 *
 * ### Common Use Cases
 * - Adding authentication tokens or API keys to request headers.
 * - Logging request details (e.g., URL, method, headers) for debugging or auditing.
 * - Rewriting or normalizing request URLs based on application logic.
 * - Enforcing rate limits or other policies before requests are sent.
 */
abstract class RequestInterceptor {

    /**
     * Intercepts and modifies the given [Request] before it is executed by the HTTP client.
     *
     * This abstract method must be implemented by subclasses to define the specific interception logic.
     * It receives a mutable [Request] object, which represents the HTTP request being prepared for execution.
     * The method can inspect or modify properties of the request, such as its URL, headers, or other attributes,
     * depending on the [Request] implementation.
     *
     * ### Parameters
     * @param request The [Request] object to intercept and potentially modify. This is typically an instance
     * of a concrete class like [GetRequest], [PostRequest], etc., which may include properties such as
     * `url`, `headers`, and (for requests with bodies) `body`.
     *
     * ### Behavior
     * - The method is called by the HTTP client for each registered interceptor in the order they were added.
     * - Modifications made to the [request] object are persisted and passed to subsequent interceptors or the
     *   final request execution.
     * - No return value is expected; changes are applied directly to the [request] parameter.
     *
     * ### Example Implementation
     * ```kotlin
     * class LoggingInterceptor : RequestInterceptor() {
     *     override fun intercept(request: Request) {
     *         println("Sending request to ${request.url} with method ${request.requestMethod}")
     *     }
     * }
     * ```
     */
    abstract fun intercept(request: Request)
}
