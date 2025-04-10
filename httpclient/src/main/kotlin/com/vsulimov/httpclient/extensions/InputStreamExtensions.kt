package com.vsulimov.httpclient.extensions

import java.io.InputStream
import java.nio.charset.Charset

/**
 * Reads the entire content of this [InputStream] as a [String] and closes the stream.
 *
 * This extension function simplifies the process of reading text from an input stream by handling
 * the creation of a [BufferedReader], reading the text, and ensuring the stream is properly closed
 * afterward. It is particularly useful for reading response bodies from HTTP requests, where the
 * input stream needs to be read fully and then closed to free system resources.
 *
 * ### Behavior
 * - The function uses a [BufferedReader] to efficiently read the input stream's content.
 * - It reads the entire content of the stream into a [String] using the specified [charset].
 * - After reading, the stream is closed automatically via the [use] function, ensuring proper
 *   resource management and preventing resource leaks.
 *
 * ### Usage
 * This function is typically used in scenarios where you need to read the full content of an input
 * stream, such as when processing HTTP response bodies. It abstracts away the boilerplate code
 * required to read and close the stream, making the code cleaner and less error-prone.
 *
 * **Example:**
 * ```kotlin
 * val inputStream: InputStream = someHttpConnection.inputStream
 * val responseBody: String = inputStream.readTextAndClose()
 * println(responseBody)
 * ```
 *
 * ### Notes
 * - If the input stream is large, this function may consume significant memory since it reads the
 *   entire content into a single [String]. For very large streams, consider processing the stream
 *   in chunks or using a streaming approach.
 * - The default charset is [Charsets.UTF_8], which is suitable for most text-based HTTP responses.
 *   If the response uses a different encoding, specify the appropriate [Charset].
 *
 * @param charset The [Charset] to use for decoding the input stream's bytes into a [String].
 *                Defaults to [Charsets.UTF_8].
 * @return The entire content of the input stream as a [String].
 */
fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String =
    this.bufferedReader(charset).use { it.readText() }
