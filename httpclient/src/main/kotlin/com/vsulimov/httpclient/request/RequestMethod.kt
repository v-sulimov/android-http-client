package com.vsulimov.httpclient.request

/**
 * Enumerates the supported HTTP request methods.
 *
 * The [RequestMethod] enum defines the standard HTTP methods supported by the HTTP client’s request classes. Each value
 * corresponds to a specific HTTP verb, which dictates the action to be performed on the server. This enum is used by
 * [Request] and its subclasses to specify the method type for each request.
 *
 * ### Values
 * - **HEAD**: Retrieves the headers of a resource without the response body (used by [HeadRequest]).
 * - **OPTIONS**: Describes the communication options available for the target resource. (used by [OptionsRequest]).
 * - **GET**: Retrieves data from the server without modifying it (used by [GetRequest]).
 * - **POST**: Submits data to the server to create a resource (used by [PostRequest]).
 * - **PUT**: Updates an existing resource on the server with the provided data (used by [PutRequest]).
 * - **PATCH**: Applies partial modifications to a resource on the server. (used by [PatchRequest]).
 * - **DELETE**: Removes a resource from the server (used by [DeleteRequest]).
 *
 * ### Purpose
 * - To provide a type-safe way to specify HTTP methods in request objects.
 * - To ensure consistency and clarity in defining request behavior.
 *
 * ### Usage
 * This enum is typically passed to the constructor of a [Request] or [RequestWithBody] subclass to set the HTTP method.
 *
 * **Example:**
 * ```kotlin
 * val getRequest = GetRequest("https://api.example.com") // Uses RequestMethod.GET
 * val postRequest = PostRequest("https://api.example.com", "{}") // Uses RequestMethod.POST
 * ```
 *
 * ### Notes
 * - The enum currently supports only GET, POST, PUT, and DELETE. Additional methods (e.g., PATCH, HEAD) could be added
 *   by extending this enum and creating corresponding request classes.
 * - The `toString()` method of each enum value returns its name (e.g., "GET"), which is used by the HTTP client to set
 *   the request method in the underlying HTTP connection.
 */
enum class RequestMethod {
    HEAD,
    OPTIONS,
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}
