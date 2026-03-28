package com.folklore25.ghosthand

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class GhosthandRequestTarget(
    val path: String,
    val queryParameters: Map<String, String>
)

object GhosthandHttp {
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
