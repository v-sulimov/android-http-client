# HttpClient for Android

A lightweight, customizable HTTP client library written in Kotlin, designed for making HTTP requests with support for
custom SSL certificates, request interceptors, and detailed response handling.

## Features

- **Supported HTTP Methods**: GET, POST, PUT, DELETE, HEAD, OPTIONS, and PATCH.
- **Custom SSL Support**: Add custom X.509 certificates for secure connections.
- **Request Interceptors**: Modify requests (e.g., add headers) before they are sent.
- **Configurable Timeouts**: Set read and connect timeouts for requests.
- **Redirect Handling**: Optionally follow HTTP redirects.
- **Type-Safe API**: Dedicated request classes for each HTTP method.
- **Error Handling**: Comprehensive exception handling for network and HTTP errors.
- **Synchronous Execution**: The client executes requests synchronously, allowing the caller to manage asynchronous execution (e.g., via coroutines or threads).

## Installation

To add the HTTP Client to your project, include the following repository in your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Other repositories here.
        maven {
            name = "repository"
            url = uri("https://maven.vsulimov.com/releases")
        }
    }
}
```

Then, include the following dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.vsulimov:httpclient:1.0.1")
}
```

## Usage

### Basic Example

Here’s how to perform a simple GET request:

```kotlin
fun main() {
    val client = HttpClient()
    val request = GetRequest("https://api.example.com/users")

    val result = client.executeGetRequest(request)
    result.onSuccess { response ->
        println("Response Code: ${response.responseCode}")
        println("Response Body: ${response.body}")
    }.onFailure { exception ->
        println("Error: ${exception.message}")
    }
}
```

### Asynchronous Usage

The `HttpClient` executes requests synchronously. To handle requests asynchronously, use Kotlin coroutines or other threading mechanisms. Example with coroutines:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() = runBlocking {
    val client = HttpClient()
    val request = GetRequest("https://api.example.com/users")

    val result = withContext(Dispatchers.IO) {
        client.executeGetRequest(request)
    }
    result.onSuccess { response ->
        println("Response Code: ${response.responseCode}")
        println("Response Body: ${response.body}")
    }.onFailure { exception ->
        println("Error: ${exception.message}")
    }
}
```

### Adding Headers

Add custom headers to a request:

```kotlin
val request = GetRequest("https://api.example.com/users").apply {
    headers.add(Header("Authorization", "Bearer token123"))
    headers.add(Header("Accept", "application/json"))
}
```

### POST Request with Body

Send a POST request with a JSON body:

```kotlin
val request = PostRequest("https://api.example.com/users", """{"name": "John Doe"}""").apply {
    headers.add(Header("Content-Type", "application/json"))
}

val result = client.executePostRequest(request)
result.onSuccess { response ->
    println("Created: ${response.body}")
}.onFailure { exception ->
    println("Failed: ${exception.message}")
}
```

### Custom SSL Configuration

Configure the client with a custom SSL certificate:

```kotlin
val certInputStream = // InputStream from your certificate file (e.g., .crt or .pem)
val config = HttpClientConfiguration.builder()
    .certificateInputStream(certInputStream)
    .build()

val client = HttpClient(config)
```

### Request Interceptors

Add an interceptor to modify requests (e.g., logging or authentication):

```kotlin
val loggingInterceptor = object : RequestInterceptor {
    override fun intercept(request: Request) {
        println("Sending ${request.requestMethod} to ${request.url}")
    }
}

client.addRequestInterceptor(loggingInterceptor)
```

### Supported Methods

The library supports the following HTTP methods with dedicated request classes:

- **GET**: `GetRequest` - Retrieve resources.
- **POST**: `PostRequest` - Submit data.
- **PUT**: `PutRequest` - Update resources.
- **DELETE**: `DeleteRequest` - Remove resources.
- **HEAD**: `HeadRequest` - Retrieve headers only.
- **OPTIONS**: `OptionsRequest` - Query server options (e.g., CORS).
- **PATCH**: `PatchRequest` - Apply partial updates.

Example for a PATCH request:

```kotlin
val request = PatchRequest("https://api.example.com/users/1", """{"name": "Jane Doe"}""")
val result = client.executePatchRequest(request)
result.onSuccess { response ->
    println("Updated: ${response.body}")
}.onFailure { exception ->
    println("Failed: ${exception.message}")
}
```

## Configuration Options

The `HttpClientConfiguration` class allows customization:

- `readTimeout`: Set the read timeout (default: 3000 ms).
- `connectTimeout`: Set the connect timeout (default: 3000 ms).
- `followRedirects`: Enable/disable redirect following (default: true).
- `certificateInputStream`: Provide a custom SSL certificate.

Example:

```kotlin
val config = HttpClientConfiguration.builder()
    .readTimeout(5000)
    .connectTimeout(5000)
    .followRedirects(false)
    .build()

val client = HttpClient(config)
```

## Error Handling

The library throws exceptions for various failure scenarios:

- `IOException`: Network-related errors (e.g., no connectivity).
- `UnsuccessfulResponseStatusCodeException`: Non-2xx status codes.
- `RedirectException`: Redirect errors when `followRedirects` is disabled.

Handle errors in the result:

```kotlin
val result = client.executeGetRequest(request)
result.onFailure { exception ->
    when (exception) {
        is UnsuccessfulResponseStatusCodeException -> println("HTTP Error: ${exception.responseCode}")
        is IOException -> println("Network Error: ${exception.message}")
        else -> println("Unknown Error: ${exception.message}")
    }
}
```

## Repository

The source code is hosted at:

```
https://git.vsulimov.com/android-http-client.git
```

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
