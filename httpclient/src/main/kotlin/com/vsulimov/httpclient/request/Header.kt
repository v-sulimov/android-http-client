package com.vsulimov.httpclient.request

/**
 * Represents an HTTP header with a name and value pair.
 *
 * The [Header] class encapsulates a single HTTP header, which consists of a name (e.g., "Content-Type")
 * and its corresponding value (e.g., "application/json"). Instances of this class are used within [Request]
 * objects to define the headers that will be sent with an HTTP request. This class is immutable, ensuring
 * that once a header is created, its properties cannot be modified, providing consistency and thread-safety.
 *
 * ### Purpose
 * - To standardize the representation of HTTP headers across different types of requests.
 * - To allow easy addition of headers to a request via a [MutableList] in [Request].
 *
 * ### Usage
 * Headers are typically added to a [Request] object’s `headers` list to customize the HTTP request.
 * For example, you might add an "Authorization" header for authentication or a "User-Agent" header
 * to identify the client.
 *
 * **Example:**
 * ```kotlin
 * val request = GetRequest("https://api.example.com")
 * request.headers.add(Header("Authorization", "Bearer my-token"))
 * ```
 *
 * ### Common Headers
 * - "Content-Type": Specifies the media type of the request or response body (e.g., "application/json").
 * - "Accept": Indicates the media types the client can handle (e.g., "application/json").
 * - "Authorization": Provides credentials for authentication (e.g., "Bearer token").
 *
 * @property name The name of the HTTP header (e.g., "Content-Type", "Authorization"). This is a
 *                case-insensitive `String` as per HTTP standards, but preserving the case can aid
 *                readability and compatibility with some servers.
 * @property value The value associated with the header name (e.g., "application/json", "Bearer my-token").
 *                 This is a `String` that can be empty but is typically non-null.
 */
class Header(val name: String, val value: String)
