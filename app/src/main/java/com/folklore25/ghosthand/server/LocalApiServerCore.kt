/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.server

import android.util.Log
import com.folklore25.ghosthand.GhosthandApiPayloads
import com.folklore25.ghosthand.GhosthandDisclosure
import com.folklore25.ghosthand.GhosthandHttp
import com.folklore25.ghosthand.LocalApiServer
import com.folklore25.ghosthand.LocalApiServerRequestException
import com.folklore25.ghosthand.RoutePolicy
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

internal data class LocalApiServerParsedRequest(
    val method: String,
    val path: String,
    val queryParameters: Map<String, String>,
    val body: String
)

internal data class LocalApiServerRouteRequest(
    val queryParameters: Map<String, String>,
    val requestBody: String
)

internal data class LocalApiServerRoute(
    val method: String,
    val path: String,
    val handler: (LocalApiServerRouteRequest) -> String
)

internal class LocalApiServerRouteRegistry(
    routes: List<LocalApiServerRoute>,
    private val pathPolicies: Map<String, RoutePolicy>
) {
    private val routesByKey = routes.associateBy { it.method to it.path }

    fun dispatch(
        method: String,
        path: String,
        queryParameters: Map<String, String>,
        requestBody: String
    ): String {
        val route = routesByKey[method to path]
        if (route != null) {
            return route.handler(
                LocalApiServerRouteRequest(
                    queryParameters = queryParameters,
                    requestBody = requestBody
                )
            )
        }

        val policy = pathPolicies[path]
        return if (policy != null) {
            LocalApiServerEnvelope.httpResponse(
                statusCode = 405,
                body = LocalApiServerEnvelope.error(
                    code = "METHOD_NOT_ALLOWED",
                    message = policy.methodNotAllowedMessage
                )
            )
        } else {
            LocalApiServerEnvelope.httpResponse(
                statusCode = 404,
                body = LocalApiServerEnvelope.error(
                    code = "NOT_FOUND",
                    message = "No endpoint matches $path."
                )
            )
        }
    }
}

internal object LocalApiServerEnvelope {
    fun success(data: JSONObject, disclosure: GhosthandDisclosure? = null): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put("data", data)
            .apply {
                disclosure?.let { put("disclosure", GhosthandApiPayloads.disclosureJson(it)) }
            }
            .put("meta", buildMeta())
    }

    fun error(
        code: String,
        message: String,
        details: JSONObject = JSONObject(),
        disclosure: GhosthandDisclosure? = null
    ): JSONObject {
        return JSONObject()
            .put("ok", false)
            .put(
                "error",
                JSONObject()
                    .put("code", code)
                    .put("message", message)
                    .put("details", details)
            )
            .apply {
                disclosure?.let { put("disclosure", GhosthandApiPayloads.disclosureJson(it)) }
            }
            .put("meta", buildMeta())
    }

    fun httpResponse(statusCode: Int, body: JSONObject): String {
        val bodyString = body.toString()
        return buildString {
            append("HTTP/1.1 ")
            append(statusCode)
            append(' ')
            append(GhosthandHttp.statusText(statusCode))
            append("\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ")
            append(bodyString.toByteArray(StandardCharsets.UTF_8).size)
            append("\r\n")
            append("Connection: close\r\n")
            append("\r\n")
            append(bodyString)
        }
    }

    fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (key, nestedValue) ->
                    if (key != null) {
                        put(key.toString(), toJsonValue(nestedValue))
                    }
                }
            }

            is Iterable<*> -> org.json.JSONArray().apply {
                value.forEach { item -> put(toJsonValue(item)) }
            }

            is Array<*> -> org.json.JSONArray().apply {
                value.forEach { item -> put(toJsonValue(item)) }
            }

            else -> value
        }
    }

    private fun buildMeta(): JSONObject {
        return JSONObject()
            .put("requestId", "req_${UUID.randomUUID().toString().replace("-", "")}")
            .put("timestamp", Instant.now().toString())
    }
}

internal object LocalApiServerProtocol {
    fun readRequest(
        inputStream: java.io.InputStream,
        maxRequestLineBytes: Int = LocalApiServer.MAX_REQUEST_LINE_BYTES,
        maxHeaderLineBytes: Int = LocalApiServer.MAX_HEADER_LINE_BYTES,
        maxHeaderCount: Int = LocalApiServer.MAX_HEADER_COUNT,
        maxHeaderBytes: Int = LocalApiServer.MAX_HEADER_BYTES,
        maxBodyBytes: Int = LocalApiServer.MAX_BODY_BYTES
    ): LocalApiServerParsedRequest {
        val requestLine = GhosthandHttp.readHttpLine(inputStream, maxRequestLineBytes)
            ?: throw LocalApiServerRequestException(
                statusCode = 400,
                errorCode = "BAD_REQUEST",
                message = "HTTP request line is required."
            )
        val requestParts = requestLine.split(" ", limit = 3)
        if (requestParts.size < 2 || requestParts[0].isBlank() || requestParts[1].isBlank()) {
            throw LocalApiServerRequestException(
                statusCode = 400,
                errorCode = "BAD_REQUEST",
                message = "HTTP request line is malformed."
            )
        }

        val headers = readHeaders(
            inputStream = inputStream,
            maxHeaderLineBytes = maxHeaderLineBytes,
            maxHeaderCount = maxHeaderCount,
            maxHeaderBytes = maxHeaderBytes
        )
        val target = GhosthandHttp.parseRequestTarget(requestParts[1])
        val requestBody = GhosthandHttp.readUtf8Body(
            inputStream = inputStream,
            contentLengthHeader = headers["content-length"],
            maxBodyBytes = maxBodyBytes
        )

        return LocalApiServerParsedRequest(
            method = requestParts[0],
            path = target.path,
            queryParameters = target.queryParameters,
            body = requestBody
        )
    }

    private fun readHeaders(
        inputStream: java.io.InputStream,
        maxHeaderLineBytes: Int,
        maxHeaderCount: Int,
        maxHeaderBytes: Int
    ): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        var headerCount = 0
        var totalHeaderBytes = 0

        while (true) {
            val headerLine = GhosthandHttp.readHttpLine(inputStream, maxHeaderLineBytes) ?: break
            if (headerLine.isBlank()) {
                return headers
            }

            headerCount += 1
            totalHeaderBytes += headerLine.toByteArray(StandardCharsets.UTF_8).size
            if (headerCount > maxHeaderCount || totalHeaderBytes > maxHeaderBytes) {
                throw LocalApiServerRequestException(
                    statusCode = 431,
                    errorCode = "HEADERS_TOO_LARGE",
                    message = "HTTP headers exceed the configured limit."
                )
            }

            val separatorIndex = headerLine.indexOf(':')
            if (separatorIndex <= 0) {
                throw LocalApiServerRequestException(
                    statusCode = 400,
                    errorCode = "BAD_REQUEST",
                    message = "HTTP header lines must contain a ':' separator."
                )
            }

            val name = headerLine.substring(0, separatorIndex).trim().lowercase()
            val value = headerLine.substring(separatorIndex + 1).trim()
            if (name.isEmpty()) {
                throw LocalApiServerRequestException(
                    statusCode = 400,
                    errorCode = "BAD_REQUEST",
                    message = "HTTP header names cannot be empty."
                )
            }
            if (name == "content-length" && headers.containsKey(name) && headers[name] != value) {
                throw LocalApiServerRequestException(
                    statusCode = 400,
                    errorCode = "BAD_REQUEST",
                    message = "Conflicting Content-Length headers are not allowed."
                )
            }

            headers[name] = value
        }

        return headers
    }
}

internal class LocalApiServerResources(
    val serverExecutor: ExecutorService,
    val clientExecutor: ExecutorService
) {
    private val activeClients = ConcurrentHashMap.newKeySet<Socket>()

    @Volatile
    private var serverSocket: ServerSocket? = null

    fun attachServerSocket(socket: ServerSocket) {
        serverSocket = socket
    }

    fun registerClient(socket: Socket) {
        activeClients.add(socket)
    }

    fun unregisterClient(socket: Socket) {
        activeClients.remove(socket)
    }

    fun hasActiveClients(): Boolean = activeClients.isNotEmpty()

    fun stopAll() {
        try {
            serverSocket?.close()
        } catch (error: Exception) {
            Log.w(LocalApiServer.LOG_TAG, "component=LocalApiServerResources operation=closeServerSocket failure=${error.javaClass.simpleName}", error)
        } finally {
            serverSocket = null
        }

        activeClients.toList().forEach { client ->
            try {
                client.close()
            } catch (error: Exception) {
                Log.w(LocalApiServer.LOG_TAG, "component=LocalApiServerResources operation=closeClientSocket failure=${error.javaClass.simpleName}", error)
            } finally {
                activeClients.remove(client)
            }
        }

        clientExecutor.shutdownNow()
        serverExecutor.shutdownNow()
    }

    fun awaitStopped(timeout: Long, unit: TimeUnit): Boolean {
        val clientStopped = clientExecutor.awaitTermination(timeout, unit)
        val serverStopped = serverExecutor.awaitTermination(timeout, unit)
        return clientStopped && serverStopped && !hasActiveClients() && serverSocket == null
    }
}
