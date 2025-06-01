package com.vsulimov.httpclient.request

/**
 * An abstract base class representing an HTTP request.
 *
 * The [Request] class serves as the foundation for all specific HTTP request types (e.g., [GetRequest], [PostRequest]).
 * It defines common properties shared by all requests, such as a list of headers, the HTTP method, and the target URL.
 * This class is abstract and cannot be instantiated directly; instead, it is extended by concrete request classes that
 * specify the HTTP method and additional properties like a request body.
 *
 * ### Purpose
 * - To provide a unified structure for all HTTP requests in the HTTP client.
 * - To allow customization of headers and URL across all request types.
 *
 * ### Properties
 * - **headers**: A mutable list of [Header] objects that can be modified to add or remove headers before the request is sent.
 * - **requestMethod**: The HTTP method (e.g., GET, POST) defined by the [RequestMethod] enum, set by subclasses.
 * - **url**: The target URL of the request, mutable to allow modification (e.g., by interceptors).
 *
 * ### Usage
 * Subclasses like [GetRequest] or [PostRequest] are instantiated to create specific request types. The `headers` list can
 * be modified to include custom headers, and the `url` can be updated if needed.
 *
 * **Example:**
 * ```kotlin
 * val request = GetRequest("https://api.example.com")
 * request.headers.add(Header("Accept", "application/json"))
 * request.url = "https://api.example.com/v2" // Modify URL if needed
 * ```
 *
 * ### Notes
 * - The `headers` list is initialized as empty but mutable, allowing dynamic header addition.
 * - The `url` is mutable to support use cases like URL rewriting by interceptors.
 *
 * @param headers A mutable list of [Header] objects representing the HTTP headers to include in the request.
 *                Defaults to an empty [MutableList].
 * @param requestMethod The HTTP method for the request, specified as a [RequestMethod] enum value (e.g., GET, POST).
 * @param url The target URL of the request as a [String] (e.g., "https://api.example.com/resource").
 */
abstract class Request(
    val headers: MutableList<Header> = mutableListOf(),
    val requestMethod: RequestMethod,
    var url: String
)

/**
 * An abstract base class for HTTP requests that include a request body.
 *
 * The [RequestWithBody] class extends [Request] to represent HTTP requests that send data in the request body, such as
 * POST and PUT requests. It adds a `body` property to store the request payload and delegates the `headers`, `requestMethod`,
 * and `url` properties to the [Request] superclass. This class is abstract and must be subclassed by concrete request types
 * like [PostRequest] or [PutRequest].
 *
 * ### Purpose
 * - To provide a common structure for requests with bodies, distinguishing them from body-less requests like GET.
 * - To encapsulate the request body as a string, which can represent JSON, plain text, or other formats.
 *
 * ### Properties
 * - **body**: The request payload as a [String], mutable to allow modification before sending.
 * - Inherits `headers`, `requestMethod`, and `url` from [Request].
 *
 * ### Usage
 * Subclasses instantiate this class with a specific HTTP method and initial body content. The `body` can be modified
 * before the request is executed.
 *
 * **Example:**
 * ```kotlin
 * val request = PostRequest("https://api.example.com", """{"name": "John"}""")
 * request.body = """{"name": "Jane"}""" // Update body if needed
 * request.headers.add(Header("Content-Type", "application/json"))
 * ```
 *
 * ### Notes
 * - The `body` is a [String], assuming the HTTP client handles encoding (e.g., to UTF-8 bytes) when sending the request.
 * - Subclasses must specify the appropriate [RequestMethod] (e.g., POST, PUT).
 *
 * @param requestMethod The HTTP method for the request, passed to the [Request] superclass (e.g., POST, PUT).
 * @param url The target URL of the request, passed to the [Request] superclass.
 * @param body The request body as a [String], representing the payload to send to the server.
 */
abstract class RequestWithBody(requestMethod: RequestMethod, url: String, var body: String) :
    Request(requestMethod = requestMethod, url = url)

/**
 * Represents an HTTP HEAD request.
 *
 * The [HeadRequest] class is a concrete implementation of [Request] for sending HTTP HEAD requests, which retrieve
 * the headers of a resource without the response body. It sets the HTTP method to [RequestMethod.HEAD] and allows
 * customization of headers and the URL. This is useful for checking resource metadata, existence, or availability
 * without downloading the full content.
 *
 * ### Usage
 * Instantiate with a URL and optionally add headers before sending via an HTTP client.
 *
 * **Example:**
 * ```kotlin
 * val request = HeadRequest("https://api.example.com/users")
 * request.headers.add(Header("Accept", "application/json"))
 * request.url = "https://api.example.com/users/1" // Modify URL if needed
 * ```
 *
 * @param url The target URL for the HEAD request (e.g., "https://api.example.com/resource").
 */
class HeadRequest(url: String) : Request(requestMethod = RequestMethod.HEAD, url = url)

/**
 * Represents an HTTP OPTIONS request.
 *
 * The [OptionsRequest] class is a concrete implementation of [Request] for sending HTTP OPTIONS requests, which describe
 * the communication options available for the target resource. It sets the HTTP method to [RequestMethod.OPTIONS] and
 * allows customization of headers and the URL. This is commonly used in CORS scenarios to determine allowed methods
 * or server capabilities.
 *
 * ### Usage
 * Instantiate with a URL and optionally add headers before sending via an HTTP client.
 *
 * **Example:**
 * ```kotlin
 * val request = OptionsRequest("https://api.example.com/users")
 * request.headers.add(Header("Origin", "https://example.com"))
 * request.headers.add(Header("Access-Control-Request-Method", "POST"))
 * ```
 *
 * ### Notes
 * - While the HTTP specification allows a body in OPTIONS requests, it is rarely used, so this implementation omits it.
 * - The response typically includes headers like `Allow` to indicate supported methods.
 *
 * @param url The target URL for the OPTIONS request (e.g., "https://api.example.com/resource").
 */
class OptionsRequest(url: String) : Request(requestMethod = RequestMethod.OPTIONS, url = url)

/**
 * Represents an HTTP GET request.
 *
 * The [GetRequest] class is a concrete implementation of [Request] for sending HTTP GET requests, which retrieve data
 * from a server without a request body. It sets the HTTP method to [RequestMethod.GET] and allows customization of headers
 * and the URL.
 *
 * ### Usage
 * Instantiate with a URL and optionally add headers before sending via an HTTP client.
 *
 * **Example:**
 * ```kotlin
 * val request = GetRequest("https://api.example.com/users")
 * request.headers.add(Header("Accept", "application/json"))
 * ```
 *
 * @param url The target URL for the GET request (e.g., "https://api.example.com/resource").
 */
class GetRequest(url: String) : Request(requestMethod = RequestMethod.GET, url = url)

/**
 * Represents an HTTP POST request with a request body.
 *
 * The [PostRequest] class is a concrete implementation of [RequestWithBody] for sending HTTP POST requests, which submit
 * data to a server. It sets the HTTP method to [RequestMethod.POST] and includes a body for the request payload.
 *
 * ### Usage
 * Instantiate with a URL and body, then optionally modify headers or body before sending.
 *
 * **Example:**
 * ```kotlin
 * val request = PostRequest("https://api.example.com/users", """{"name": "John"}""")
 * request.headers.add(Header("Content-Type", "application/json"))
 * ```
 *
 * @param url The target URL for the POST request (e.g., "https://api.example.com/resource").
 * @param body The request body as a [String] (e.g., a JSON string like """{"key": "value"}""").
 */
class PostRequest(url: String, body: String) :
    RequestWithBody(requestMethod = RequestMethod.POST, url = url, body = body)

/**
 * Represents an HTTP PUT request with a request body.
 *
 * The [PutRequest] class is a concrete implementation of [RequestWithBody] for sending HTTP PUT requests, which update
 * data on a server. It sets the HTTP method to [RequestMethod.PUT] and includes a body for the request payload.
 *
 * ### Usage
 * Instantiate with a URL and body, then optionally modify headers or body before sending.
 *
 * **Example:**
 * ```kotlin
 * val request = PutRequest("https://api.example.com/users/1", """{"name": "Jane"}""")
 * request.headers.add(Header("Content-Type", "application/json"))
 * ```
 *
 * @param url The target URL for the PUT request (e.g., "https://api.example.com/resource/1").
 * @param body The request body as a [String] (e.g., a JSON string like """{"key": "value"}""").
 */
class PutRequest(url: String, body: String) : RequestWithBody(requestMethod = RequestMethod.PUT, url = url, body = body)

/**
 * Represents an HTTP PATCH request with a request body.
 *
 * The [PatchRequest] class is a concrete implementation of [RequestWithBody] for sending HTTP PATCH requests, which apply
 * partial modifications to a resource on the server. It sets the HTTP method to [RequestMethod.PATCH] and includes a body
 * for the request payload. Unlike PUT, which replaces an entire resource, PATCH updates specific parts, making it ideal
 * for efficient resource modifications.
 *
 * ### Usage
 * Instantiate with a URL and body, then optionally modify headers or body before sending.
 *
 * **Example:**
 * ```kotlin
 * val request = PatchRequest("https://api.example.com/users/1", """{"name": "Jane"}""")
 * request.headers.add(Header("Content-Type", "application/json"))
 * request.body = """{"name": "Jane Doe"}""" // Update body if needed
 * ```
 *
 * @param url The target URL for the PATCH request (e.g., "https://api.example.com/resource/1").
 * @param body The request body as a [String] (e.g., a JSON string like """{"key": "value"}""").
 */
class PatchRequest(url: String, body: String) :
    RequestWithBody(requestMethod = RequestMethod.PATCH, url = url, body = body)

/**
 * Represents an HTTP DELETE request.
 *
 * The [DeleteRequest] class is a concrete implementation of [Request] for sending HTTP DELETE requests, which remove
 * resources from a server. It sets the HTTP method to [RequestMethod.DELETE] and does not include a request body.
 *
 * ### Usage
 * Instantiate with a URL and optionally add headers before sending.
 *
 * **Example:**
 * ```kotlin
 * val request = DeleteRequest("https://api.example.com/users/1")
 * request.headers.add(Header("Authorization", "Bearer token123"))
 * ```
 *
 * @param url The target URL for the DELETE request (e.g., "https://api.example.com/resource/1").
 */
class DeleteRequest(url: String) : Request(requestMethod = RequestMethod.DELETE, url = url)
