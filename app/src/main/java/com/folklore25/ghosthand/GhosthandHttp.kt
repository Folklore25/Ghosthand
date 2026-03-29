/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class GhosthandRequestTarget(
    val path: String,
    val queryParameters: Map<String, String>
)

object GhosthandHttp {
    fun readHttpLine(inputStream: InputStream, maxBytes: Int = Int.MAX_VALUE): String? {
        val bytes = ArrayList<Byte>(64)

        while (true) {
            val next = inputStream.read()
            if (next == -1) {
                return if (bytes.isEmpty()) null else bytes.toByteArray().toString(StandardCharsets.UTF_8)
            }

            if (next == '\n'.code) {
                break
            }

            if (next != '\r'.code) {
                bytes.add(next.toByte())
                if (bytes.size > maxBytes) {
                    throw LocalApiServerRequestException(
                        statusCode = 431,
                        errorCode = "HEADERS_TOO_LARGE",
                        message = "HTTP line exceeds the configured limit."
                    )
                }
            }
        }

        return bytes.toByteArray().toString(StandardCharsets.UTF_8)
    }

    fun readUtf8Body(
        inputStream: InputStream,
        contentLengthHeader: String?,
        maxBodyBytes: Int = Int.MAX_VALUE
    ): String {
        val normalizedHeader = contentLengthHeader?.trim()
        if (normalizedHeader.isNullOrEmpty()) {
            return ""
        }

        val contentLength = normalizedHeader.toIntOrNull()
            ?: throw LocalApiServerRequestException(
                statusCode = 400,
                errorCode = "BAD_REQUEST",
                message = "Content-Length must be a valid integer."
            )

        if (contentLength <= 0) {
            return ""
        }
        if (contentLength > maxBodyBytes) {
            throw LocalApiServerRequestException(
                statusCode = 413,
                errorCode = "REQUEST_TOO_LARGE",
                message = "Request body exceeds the configured limit."
            )
        }

        val bodyBytes = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val read = inputStream.read(bodyBytes, offset, contentLength - offset)
            if (read == -1) {
                throw LocalApiServerRequestException(
                    statusCode = 400,
                    errorCode = "BAD_REQUEST",
                    message = "Request body ended before Content-Length bytes were received."
                )
            }
            offset += read
        }

        return String(bodyBytes, 0, offset, StandardCharsets.UTF_8)
    }

    fun parseRequestTarget(requestTarget: String): GhosthandRequestTarget {
        return GhosthandRequestTarget(
            path = requestTarget.substringBefore('?'),
            queryParameters = parseQueryParameters(requestTarget)
        )
    }

    fun statusText(statusCode: Int): String {
        return when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            408 -> "Request Timeout"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            413 -> "Payload Too Large"
            422 -> "Unprocessable Entity"
            431 -> "Request Header Fields Too Large"
            503 -> "Service Unavailable"
            500 -> "Internal Server Error"
            else -> "Internal Server Error"
        }
    }

    fun parseQueryParameters(requestTarget: String): Map<String, String> {
        val query = requestTarget.substringAfter('?', "")
        if (query.isEmpty()) {
            return emptyMap()
        }

        return buildMap {
            query.split('&')
                .filter { it.isNotEmpty() }
                .forEach { entry ->
                    val key = entry.substringBefore('=')
                    if (key.isEmpty()) {
                        return@forEach
                    }

                    put(
                        URLDecoder.decode(key, StandardCharsets.UTF_8.name()),
                        URLDecoder.decode(entry.substringAfter('=', ""), StandardCharsets.UTF_8.name())
                    )
            }
        }
    }
}
