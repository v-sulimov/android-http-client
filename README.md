# android-http-client

A lightweight Android HTTP client library written in Kotlin with zero external dependencies.

Built entirely around `HttpURLConnection`, android-http-client covers GET, POST, PUT, DELETE, HEAD,
OPTIONS, and PATCH with a consistent, type-safe API that wraps every outcome in a Kotlin `Result`.
Streaming variants handle large uploads and downloads without buffering the payload in memory.

---

## Table of Contents

- [Overview](#overview)
- [Requirements](#requirements)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [HTTP Methods](#http-methods)
- [Response](#response)
- [Error Handling](#error-handling)
- [Request Interceptors](#request-interceptors)
- [Streaming I/O](#streaming-io)
- [Custom SSL Certificates](#custom-ssl-certificates)
- [Redirect Handling](#redirect-handling)
- [Thread Safety](#thread-safety)
- [Architecture](#architecture)
- [License](#license)

---

## Overview

android-http-client provides a single public entry point — `HttpClient` — that exposes every
supported HTTP method as a blocking function returning `Result<Response>`. The library handles:

- Opening and closing `HttpURLConnection` instances with proper resource management
- Applying registered request interceptors before sending each request
- Following redirects up to a configurable depth, with sensitive header stripping on cross-origin hops
- Custom X.509 certificate trust in addition to the system trust store
- Streaming uploads and downloads via `InputStream`, avoiding full in-memory buffering
- Returning all network and HTTP errors as `Result.failure` — no unchecked exceptions escape the public API

The library has **zero external dependencies**. It uses only the Android SDK and the Java standard
library.

---

## Requirements

- **Android**: minimum SDK 24 (Android 7.0 Nougat)
- **Kotlin**: 1.9 or newer
- **JVM target**: 11

---

## Installation

Add the Maven repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "vsulimov"
            url = uri("https://maven.vsulimov.com/releases")
        }
    }
}
```

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.vsulimov:httpclient:1.0.0")
}
```

---

## Getting Started

Create an `HttpClient` instance and execute a request. All methods are blocking — call them from a
background thread or a coroutine dispatcher such as `Dispatchers.IO`.

```kotlin
val client = HttpClient()

val result = client.executeGetRequest(GetRequest("https://api.example.com/users"))

result
    .onSuccess { response ->
        println("Status: ${response.statusCode}")
        println("Body:   ${response.body}")
    }
    .onFailure { exception ->
        println("Error: ${exception.message}")
    }
```

**With coroutines:**

```kotlin
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        client.executeGetRequest(GetRequest("https://api.example.com/users"))
    }
    result.onSuccess { handleResponse(it) }
          .onFailure { handleError(it) }
}
```

---

## Configuration

Pass an `HttpClientConfiguration` to the `HttpClient` constructor to override defaults. Use the
builder to set only the values you need; unset properties retain their defaults.

```kotlin
val config = HttpClientConfiguration.builder()
    .setReadTimeout(10_000)       // ms; default 3000
    .setConnectTimeout(5_000)     // ms; default 3000
    .setFollowRedirects(true)     // default true
    .setMaxRedirects(5)           // default 10
    .build()

val client = HttpClient(config)
```

To derive a modified configuration from an existing one without repeating unchanged values, use
`toBuilder()`:

```kotlin
val extended = config.toBuilder()
    .setReadTimeout(30_000)
    .build()
```

### Configuration properties

| Property                 | Type          | Default | Description                                                                              |
|--------------------------|---------------|---------|------------------------------------------------------------------------------------------|
| `readTimeout`            | `Int` (ms)    | `3000`  | Maximum time to wait for data after a connection is established. `0` means no timeout.   |
| `connectTimeout`         | `Int` (ms)    | `3000`  | Maximum time to wait for the TCP connection to be established. `0` means no timeout.     |
| `followRedirects`        | `Boolean`     | `true`  | Whether 3xx responses are followed automatically.                                         |
| `maxRedirects`           | `Int`         | `10`    | Maximum number of consecutive redirects before failing with `RedirectException`.         |
| `certificateInputStream` | `InputStream?`| `null`  | An X.509 certificate stream for custom SSL trust. `null` uses the system trust store only. |

---

## HTTP Methods

Each HTTP method has a dedicated request class and a corresponding execute method on `HttpClient`.

### Body-less requests

```kotlin
// GET
client.executeGetRequest(GetRequest("https://api.example.com/users"))

// HEAD — response body is always empty; headers contain the metadata
client.executeHeadRequest(HeadRequest("https://api.example.com/users"))

// OPTIONS
client.executeOptionsRequest(OptionsRequest("https://api.example.com/users"))

// DELETE
client.executeDeleteRequest(DeleteRequest("https://api.example.com/users/1"))
```

### Requests with a body

The body is a `String` encoded to UTF-8 bytes before transmission.

```kotlin
// POST
val post = PostRequest(
    url = "https://api.example.com/users",
    body = """{"name": "Alice"}"""
).apply {
    headers.add(Header("Content-Type", "application/json"))
}
client.executePostRequest(post)

// PUT
val put = PutRequest(
    url = "https://api.example.com/users/1",
    body = """{"name": "Bob"}"""
).apply {
    headers.add(Header("Content-Type", "application/json"))
}
client.executePutRequest(put)

// PATCH
val patch = PatchRequest(
    url = "https://api.example.com/users/1",
    body = """{"name": "Carol"}"""
).apply {
    headers.add(Header("Content-Type", "application/json"))
}
client.executePatchRequest(patch)
```

### Adding headers

Headers are added to the `headers` list on any request object:

```kotlin
val request = GetRequest("https://api.example.com/users").apply {
    headers.add(Header("Authorization", "Bearer token123"))
    headers.add(Header("Accept", "application/json"))
}
```

---

## Response

A successful result contains a `Response` with three properties:

| Property     | Type                  | Description                                                                                     |
|--------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `statusCode` | `Int`                 | HTTP status code (e.g., `200`, `201`).                                                          |
| `body`       | `String`              | Response body decoded as UTF-8. Empty string for responses with no body (e.g., `204 No Content`). |
| `headers`    | `Map<String, String>` | Response headers. Keys are **case-insensitive** (RFC 7230). Multi-value headers are joined with `", "`. |

```kotlin
result.onSuccess { response ->
    val contentType = response.headers["content-type"] // case-insensitive lookup
    val body = response.body
}
```

---

## Error Handling

All execute methods return `Result<Response>`. Exceptions are never thrown from the public API —
every failure is wrapped in `Result.failure`. Use `onFailure` or `exceptionOrNull` to inspect errors.

Three exception types may appear in a failure result:

### `UnsuccessfulResponseStatusCodeException`

Thrown when the server responds with a status code outside the 2xx range (e.g., `404`, `500`).

```kotlin
result.onFailure { exception ->
    if (exception is UnsuccessfulResponseStatusCodeException) {
        println("HTTP ${exception.message}")
        println("Error body: ${exception.errorBody}")
    }
}
```

### `RedirectException`

Thrown when a redirect response is received and `followRedirects` is `false`, when the `Location`
header is absent, or when the redirect chain exceeds `maxRedirects`.

```kotlin
result.onFailure { exception ->
    if (exception is RedirectException) {
        println("Redirect: ${exception.message}")
    }
}
```

### `IOException`

Thrown for network-level failures such as no connectivity, DNS resolution failure, or connection
timeout.

```kotlin
result.onFailure { exception ->
    when (exception) {
        is UnsuccessfulResponseStatusCodeException -> showHttpError(exception.errorBody)
        is RedirectException                       -> showRedirectError(exception.message)
        is IOException                             -> showNetworkError(exception.message)
    }
}
```

---

## Request Interceptors

Interceptors are invoked once per user-initiated request, before the connection is opened. They
receive the mutable `Request` object and may modify its `url`, `headers`, or `body` in place.
Multiple interceptors are executed in the order they were added.

```kotlin
class AuthInterceptor(private val token: String) : RequestInterceptor() {
    override fun intercept(request: Request) {
        request.headers.add(Header("Authorization", "Bearer $token"))
    }
}

class LoggingInterceptor : RequestInterceptor() {
    override fun intercept(request: Request) {
        println("→ ${request.requestMethod} ${request.url}")
    }
}

client.addRequestInterceptor(AuthInterceptor("my-token"))
client.addRequestInterceptor(LoggingInterceptor())
```

Interceptors are **not** re-applied when following redirects — they run exactly once on the
original request. Managing interceptors is thread-safe.

| Method                        | Description                                 |
|-------------------------------|---------------------------------------------|
| `addRequestInterceptor(i)`    | Appends an interceptor to the chain.        |
| `removeRequestInterceptor(i)` | Removes a specific interceptor.             |
| `removeAllRequestInterceptors()` | Clears all registered interceptors.      |

---

## Streaming I/O

Streaming variants transmit or receive data via `InputStream` without buffering the full payload in
memory. They accept a `streamHandler` lambda that receives a `StreamingResponse` and returns a value
of type `T`. The result wraps whatever `T` the lambda returns.

The connection is held open for the duration of the lambda and disconnected immediately after it
returns. **Do not retain a reference to `StreamingResponse.body` beyond the scope of the handler.**

### Streaming download

```kotlin
val request = GetRequest("https://api.example.com/files/report.pdf").apply {
    headers.add(Header("Authorization", "Bearer token123"))
}

val result = client.executeStreamingGetRequest(request) { response ->
    File("report.pdf").outputStream().use { response.body.copyTo(it) }
}

result.onFailure { println("Download failed: ${it.message}") }
```

### Streaming upload

Upload bodies are transmitted using chunked transfer encoding; the total size does not need to be
known in advance.

```kotlin
val request = StreamingPostRequest(
    url = "https://api.example.com/upload",
    bodyStream = File("video.mp4").inputStream()
).apply {
    headers.add(Header("Content-Type", "video/mp4"))
}

val result = client.executeStreamingPostRequest(request) { response ->
    response.statusCode // value returned as Result<Int>
}
```

### Streaming methods

| Method                          | Upload source              |
|---------------------------------|----------------------------|
| `executeStreamingGetRequest`    | — (download only)          |
| `executeStreamingPostRequest`   | `StreamingPostRequest.bodyStream`  |
| `executeStreamingPutRequest`    | `StreamingPutRequest.bodyStream`   |
| `executeStreamingPatchRequest`  | `StreamingPatchRequest.bodyStream` |

Redirect following is not performed for streaming requests. A 3xx response surfaces as
`RedirectException`.

---

## Custom SSL Certificates

To trust a custom CA certificate (e.g., a self-signed certificate for an internal server), load it
as an `InputStream` and pass it to the configuration builder. The client will trust certificates
issued by both the system trust store and the provided CA.

```kotlin
val certStream = context.resources.openRawResource(R.raw.my_ca_cert) // .crt or .pem
val config = HttpClientConfiguration.builder()
    .setCertificateInputStream(certStream)
    .build()

val client = HttpClient(config)
```

The certificate stream is read exactly once during `HttpClient` construction and closed automatically.

---

## Redirect Handling

When `followRedirects` is `true` (the default), 3xx responses are followed automatically up to
`maxRedirects` hops. Redirect following always issues a `GET` request to the `Location` URL,
regardless of the original method.

**Security**: headers are forwarded selectively on redirects. On same-host redirects, all headers
are preserved. On cross-origin redirects, the following sensitive headers are stripped to prevent
credential leakage:

- `Authorization`
- `Cookie`
- `Proxy-Authorization`
- `Set-Cookie`
- `Proxy-Authenticate`
- `WWW-Authenticate`

Relative `Location` URLs are resolved against the original request URL.

When `followRedirects` is `false`, any 3xx response fails immediately with `RedirectException`.

---

## Thread Safety

`HttpClient` is thread-safe. A single instance may be shared across threads and used to execute
concurrent requests without external synchronization.

- The `SSLSocketFactory` is built once during construction and never mutated.
- The interceptor list uses `CopyOnWriteArrayList`, making all interceptor management methods safe
  to call concurrently with in-flight requests.
- All request and response objects are independent per call and carry no shared mutable state.

---

## Architecture

### Package structure

```
com.vsulimov.httpclient
├── HttpClient.kt                        — public entry point
├── configuration/
│   └── HttpClientConfiguration.kt      — immutable configuration + builder
├── request/
│   ├── Request.kt                       — abstract base and all concrete request classes
│   ├── StreamingRequest.kt              — streaming request variants
│   ├── RequestMethod.kt                 — HTTP method enum
│   └── Header.kt                        — name/value header pair
├── response/
│   ├── Response.kt                      — buffered response (statusCode, body, headers)
│   └── StreamingResponse.kt             — streaming response (statusCode, headers, body: InputStream)
├── interceptor/
│   └── RequestInterceptor.kt            — abstract interceptor base class
├── security/
│   └── CompositeX509TrustManager.kt     — system + custom certificate trust composition
├── exception/
│   ├── RedirectException.kt             — thrown on unresolvable or disabled redirects
│   └── UnsuccessfulResponseStatusCodeException.kt — thrown for non-2xx status codes
└── extensions/
    └── InputStreamExtensions.kt         — readTextAndClose() utility
```

### Request lifecycle

1. The caller constructs a typed request object and calls the corresponding `execute…` method.
2. `HttpClient` applies all registered interceptors to the request exactly once.
3. A new `HttpURLConnection` is opened for the request URL.
4. For HTTPS connections, the custom `SSLSocketFactory` is installed.
5. Request headers and body (if any) are written to the connection.
6. The response code is read and dispatched:
   - **2xx** → body is read and a `Response` is returned as `Result.success`.
   - **3xx with `followRedirects = true`** → the original connection is eagerly disconnected and a
     new request is issued recursively to the resolved `Location` URL.
   - **3xx with `followRedirects = false`** → `RedirectException` is returned as `Result.failure`.
   - **other** → `UnsuccessfulResponseStatusCodeException` is returned as `Result.failure`.
7. The connection is disconnected in a `finally` block regardless of outcome.

### SSL trust composition

`CompositeX509TrustManager` combines the system default trust manager with any custom trust managers
derived from provided `KeyStore` instances. Certificate validation succeeds if any one trust manager
accepts the chain. Accepted issuers are deduplicated by subject distinguished name.

---

## License

Copyright (c) 2026 Vitaly Sulimov

Licensed under the MIT License. See [LICENSE](LICENSE) for full details.
