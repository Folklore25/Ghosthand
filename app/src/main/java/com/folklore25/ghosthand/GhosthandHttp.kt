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
    fun readHttpLine(inputStream: InputStream): String? {
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
            }
        }

        return bytes.toByteArray().toString(StandardCharsets.UTF_8)
    }

    fun readUtf8Body(inputStream: InputStream, contentLengthHeader: String?): String {
        val contentLength = contentLengthHeader?.toIntOrNull() ?: return ""
        if (contentLength <= 0) {
            return ""
        }

        val bodyBytes = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val read = inputStream.read(bodyBytes, offset, contentLength - offset)
            if (read == -1) {
                break
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
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            422 -> "Unprocessable Entity"
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
