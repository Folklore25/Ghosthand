/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.accessibilityservice.AccessibilityService
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

internal data class LocalApiServerParsedRequest(
    val method: String,
    val path: String,
    val queryParameters: Map<String, String>,
    val body: String
)

internal class LocalApiServerRequestException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String
) : IllegalArgumentException(message)

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

class LocalApiServer(
    context: android.content.Context,
    runtimeStateProvider: () -> RuntimeState
) {
    private val stateCoordinator = StateCoordinator(
        context = context.applicationContext,
        runtimeStateProvider = runtimeStateProvider
    )

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        stateCoordinator.setMediaProjection(projection)
    }

    fun hasMediaProjection(): Boolean = stateCoordinator.hasMediaProjection()
    private val running = AtomicBoolean(false)
    @Volatile
    private var resources = createResources()

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        if (resources.serverExecutor.isShutdown || resources.clientExecutor.isShutdown) {
            resources = createResources()
        }

        val localResources = resources
        localResources.serverExecutor.execute {
            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(InetAddress.getByName(HOST), PORT))
                }
                localResources.attachServerSocket(socket)
                RuntimeStateStore.markLocalApiServerStarted()
                Log.i(LOG_TAG, "Listening on $HOST:$PORT")

                while (running.get()) {
                    val client = socket.accept()
                    client.soTimeout = CLIENT_READ_TIMEOUT_MS
                    localResources.registerClient(client)
                    try {
                        localResources.clientExecutor.execute {
                            handleClient(client, localResources)
                        }
                    } catch (error: RejectedExecutionException) {
                        localResources.unregisterClient(client)
                        rejectBusyClient(client)
                    }
                }
            } catch (error: SocketException) {
                if (running.get()) {
                    RuntimeStateStore.markLocalApiServerFailed(error.message ?: "socket error")
                    Log.e(LOG_TAG, "Socket failure", error)
                }
            } catch (error: Exception) {
                if (running.get()) {
                    RuntimeStateStore.markLocalApiServerFailed(error.message ?: "unknown error")
                    Log.e(LOG_TAG, "Startup failure", error)
                }
            } finally {
                localResources.stopAll()
                running.set(false)
                RuntimeStateStore.markLocalApiServerStopped()
                Log.i(LOG_TAG, "Stopped")
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }
        resources.stopAll()
        if (!resources.awaitStopped(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Log.w(LOG_TAG, "Timed out waiting for LocalApiServer executors to stop")
        }
    }

    private fun handleClient(socket: Socket, localResources: LocalApiServerResources) {
        socket.use { client ->
            try {
                val request = LocalApiServerProtocol.readRequest(client.getInputStream())
                val method = request.method
                val path = request.path
                val queryParameters = request.queryParameters
                val requestBody = request.body

                val response = CapabilityRoutePolicy.policyDeniedResponse(
                    path = path,
                    capabilityAccess = stateCoordinator.capabilityAccessSnapshot()
                )?.let { denied ->
                    buildJsonResponse(
                        statusCode = 403,
                        body = errorEnvelope(
                            code = "CAPABILITY_POLICY_DENIED",
                            message = denied
                        )
                    )
                } ?: when {
                    method == "GET" && path == "/ping" -> buildPingResponse()
                    method == "GET" && path == "/health" -> buildHealthResponse()
                    method == "GET" && path == "/commands" -> buildCommandsResponse()
                    method == "GET" && path == "/state" -> buildStateResponse()
                    method == "GET" && path == "/device" -> buildDeviceResponse()
                    method == "GET" && path == "/foreground" -> buildForegroundResponse()
                    method == "GET" && path == "/tree" -> buildTreeResponse(queryParameters)
                    method == "POST" && path == "/find" -> buildFindResponse(requestBody)
                    method == "POST" && path == "/tap" -> buildTapResponse(requestBody)
                    method == "POST" && path == "/swipe" -> buildSwipeResponse(requestBody)
                    method == "POST" && path == "/type" -> buildTypeResponse(requestBody)
                    method == "GET" && path == "/screen" -> buildScreenResponse(queryParameters)
                    method == "GET" && path == "/screenshot" -> buildScreenshotGetResponse(queryParameters)
                    method == "POST" && path == "/screenshot" -> buildScreenshotResponse(requestBody)
                    method == "GET" && path == "/info" -> buildInfoResponse()
                    method == "GET" && path == "/focused" -> buildFocusedResponse()
                    method == "POST" && path == "/click" -> buildClickResponse(requestBody)
                    method == "POST" && path == "/input" -> buildInputResponse(requestBody)
                    method == "POST" && path == "/setText" -> buildSetTextResponse(requestBody)
                    method == "POST" && path == "/scroll" -> buildScrollResponse(requestBody)
                    method == "POST" && path == "/longpress" -> buildLongpressResponse(requestBody)
                    method == "POST" && path == "/gesture" -> buildGestureResponse(requestBody)
                    method == "POST" && path == "/launch" -> buildLaunchResponse(requestBody)
                    method == "POST" && path == "/back" -> buildGlobalActionResponse("back", AccessibilityService.GLOBAL_ACTION_BACK)
                    method == "POST" && path == "/home" -> buildGlobalActionResponse("home", AccessibilityService.GLOBAL_ACTION_HOME)
                    method == "POST" && path == "/recents" -> buildGlobalActionResponse("recents", AccessibilityService.GLOBAL_ACTION_RECENTS)
                    method == "GET" && path == "/clipboard" -> buildClipboardReadResponse()
                    method == "POST" && path == "/clipboard" -> buildClipboardWriteResponse(requestBody)
                    method == "GET" && path == "/wait" -> buildWaitUiChangeResponse(queryParameters)
                    method == "POST" && path == "/wait" -> buildWaitResponse(requestBody)
                    method == "GET" && path == "/notify" -> buildNotifyReadResponse(queryParameters)
                    method == "POST" && path == "/notify" -> buildNotifyPostResponse(requestBody)
                    method == "DELETE" && path == "/notify" -> buildNotifyCancelResponse(requestBody)
                    GhosthandRoutePolicies.policyFor(path) != null -> buildJsonResponse(
                        statusCode = 405,
                        body = errorEnvelope(
                            code = "METHOD_NOT_ALLOWED",
                            message = GhosthandRoutePolicies.policyFor(path)!!.methodNotAllowedMessage
                        )
                    )
                    else -> buildJsonResponse(
                        statusCode = 404,
                        body = errorEnvelope(
                            code = "NOT_FOUND",
                            message = "No endpoint matches $path."
                        )
                    )
                }

                OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
                    writer.write(response)
                    writer.flush()
                }
            } catch (error: LocalApiServerRequestException) {
                Log.w(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient status=${error.statusCode} code=${error.errorCode} failure=${error.javaClass.simpleName} message=${error.message}"
                )
                writeResponse(
                    client,
                    buildJsonResponse(
                        statusCode = error.statusCode,
                        body = errorEnvelope(
                            code = error.errorCode,
                            message = error.message
                        )
                    )
                )
            } catch (error: SocketTimeoutException) {
                Log.w(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient failure=${error.javaClass.simpleName} message=Client read timed out"
                )
                writeResponse(
                    client,
                    buildJsonResponse(
                        statusCode = 408,
                        body = errorEnvelope(
                            code = "REQUEST_TIMEOUT",
                            message = "Timed out waiting for the full HTTP request."
                        )
                    )
                )
            } catch (error: Exception) {
                Log.e(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient failure=${error.javaClass.simpleName}",
                    error
                )
                writeResponse(
                    client,
                    buildJsonResponse(
                        statusCode = 500,
                        body = errorEnvelope(
                            code = "INTERNAL_ERROR",
                            message = "Failed to handle request."
                        )
                    )
                )
            } finally {
                localResources.unregisterClient(client)
            }
        }
    }

    private fun rejectBusyClient(client: Socket) {
        client.use {
            writeResponse(
                it,
                buildJsonResponse(
                    statusCode = 503,
                    body = errorEnvelope(
                        code = "SERVER_BUSY",
                        message = "Local API server is at capacity. Retry the request."
                    )
                )
            )
        }
    }

    private fun writeResponse(client: Socket, response: String) {
        try {
            OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
                writer.write(response)
                writer.flush()
            }
        } catch (error: Exception) {
            Log.w(
                LOG_TAG,
                "component=LocalApiServer operation=writeResponse failure=${error.javaClass.simpleName}",
                error
            )
        }
    }

    private fun buildHealthResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createHealthPayload())
        )
    }

    private fun buildCommandsResponse(): String {
        val commands = org.json.JSONArray()
        GhosthandCommandCatalog.commands.forEach { command ->
            val params = org.json.JSONArray()
            command.params.forEach { param ->
                params.put(
                    JSONObject()
                        .put("name", param.name)
                        .put("type", param.type)
                        .put("location", param.location)
                        .put("required", param.required)
                        .put("description", param.description)
                        .put("allowedValues", org.json.JSONArray(param.allowedValues))
                )
            }

            commands.put(
                JSONObject()
                    .put("id", command.id)
                    .put("category", command.category)
                    .put("method", command.method)
                    .put("path", command.path)
                    .put("description", command.description)
                    .put("params", params)
                    .put("responseFields", org.json.JSONArray(command.responseFields))
                    .put(
                        "selectorSupport",
                        command.selectorSupport?.let { selectorSupport ->
                            JSONObject()
                                .put("aliases", org.json.JSONArray(selectorSupport.aliases))
                                .put("strategies", org.json.JSONArray(selectorSupport.strategies))
                                .put("primaryStrategies", org.json.JSONArray(selectorSupport.primaryStrategies))
                                .put("boundedAids", org.json.JSONArray(selectorSupport.boundedAids))
                        } ?: JSONObject.NULL
                    )
                    .put("focusRequirement", command.focusRequirement)
                    .put("delayedAcceptance", command.delayedAcceptance)
                    .put("transportContract", command.transportContract)
                    .put("stateTruth", command.stateTruth)
                    .put("changeSignal", command.changeSignal)
                    .put("operatorUses", org.json.JSONArray(command.operatorUses))
                    .put("referenceStability", command.referenceStability)
                    .put("snapshotScope", command.snapshotScope)
                    .put("recommendedInteractionModel", command.recommendedInteractionModel)
                    .put("stability", command.stability)
                    .put("exampleRequest", command.exampleRequest?.let(::toJsonValue) ?: JSONObject.NULL)
                    .put("exampleResponse", command.exampleResponse?.let(::toJsonValue) ?: JSONObject.NULL)
            )
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                JSONObject()
                    .put("schemaVersion", GhosthandCommandCatalog.schemaVersion)
                    .put(
                        "selectorAliases",
                        JSONObject().apply {
                            GhosthandCommandCatalog.selectorAliases.forEach { (alias, strategy) ->
                                put(alias, strategy)
                            }
                        }
                    )
                    .put(
                        "selectorStrategies",
                        org.json.JSONArray(GhosthandCommandCatalog.selectorStrategies)
                    )
                    .put("commands", commands)
            )
        )
    }

    private fun buildPingResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createPingPayload())
        )
    }

    private fun buildStateResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createStatePayload())
        )
    }

    private fun toJsonValue(value: Any?): Any {
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
                value.forEach { item ->
                    put(toJsonValue(item))
                }
            }
            is Array<*> -> org.json.JSONArray().apply {
                value.forEach { item ->
                    put(toJsonValue(item))
                }
            }
            else -> value
        }
    }

    private fun buildDeviceResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createDevicePayload())
        )
    }

    private fun buildForegroundResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createForegroundPayload())
        )
    }

    private fun buildTreeResponse(queryParameters: Map<String, String>): String {
        val mode = queryParameters["mode"]?.ifBlank { DEFAULT_TREE_MODE } ?: DEFAULT_TREE_MODE
        if (mode != TREE_MODE_FLAT && mode != TREE_MODE_RAW) {
            return buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "UNSUPPORTED_OPERATION",
                    message = "Only mode=flat and mode=raw are supported for /tree."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                stateCoordinator.createTreePayload(
                    treeSnapshotResult.snapshot ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT),
                    mode = mode
                )
            )
        )
    }

    private fun buildFindResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/find")
            ?: return badJsonBodyResponse()

        val selector = parseSelector(body)
        if (selector == null) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "One of text, desc, id, or strategy is required."
                )
            )
        }

        if (selector.strategy !in SUPPORTED_FIND_STRATEGIES) {
            return buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "UNSUPPORTED_OPERATION",
                    message = "Unsupported /find strategy: ${selector.strategy}."
                )
            )
        }

        if (selector.strategy == FIND_STRATEGY_FOCUSED && !selector.query.isNullOrBlank()) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "query must be omitted or null for focused strategy."
                )
            )
        }

        if (selector.strategy != FIND_STRATEGY_FOCUSED && selector.query.isNullOrBlank()) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "query is required for strategy ${selector.strategy}."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
            ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)
        val clickableOnly = body.optBoolean("clickable", false)
        val index = body.optIntOrNull("index") ?: 0
        val result = stateCoordinator.findResult(
            snapshot = snapshot,
            strategy = selector.strategy,
            query = selector.query,
            clickableOnly = clickableOnly,
            index = index
        )
        val payload = GhosthandApiPayloads.findPayload(result)

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                data = payload,
                disclosure = buildFindDisclosure(
                    strategy = selector.strategy,
                    clickableOnly = clickableOnly,
                    result = result
                )
            )
        )
    }

    private fun buildTreeUnavailableResponse(reason: TreeUnavailableReason?): String {
        val message = when (reason) {
            TreeUnavailableReason.ACCESSIBILITY_SERVICE_DISCONNECTED ->
                "Accessibility service is unavailable or not connected."
            TreeUnavailableReason.NO_ACTIVE_ROOT ->
                "Accessibility tree is unavailable because no active window root is available."
            null ->
                "Accessibility tree is unavailable."
        }

        return buildJsonResponse(
            statusCode = 503,
            body = errorEnvelope(
                code = "ACCESSIBILITY_UNAVAILABLE",
                message = message
            )
        )
    }

    private fun buildTapResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/tap")
            ?: return badJsonBodyResponse()

        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> {
                return buildJsonResponse(
                    statusCode = 422,
                    body = errorEnvelope(
                        code = "UNSUPPORTED_OPERATION",
                        message = "Only backend=accessibility and backend=auto are supported."
                    )
                )
            }
        }

        val directX = body.optIntOrNull("x")
        val directY = body.optIntOrNull("y")

        val tapResult = if (directX != null && directY != null) {
            stateCoordinator.tapPoint(directX, directY)
        } else {
            val target = body.optJSONObject("target") ?: return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "Either x/y or target is required."
                )
            )

            val targetType = target.optString("type").trim()
            if (targetType.isEmpty()) {
                return buildJsonResponse(
                    statusCode = 400,
                    body = errorEnvelope(
                        code = "INVALID_ARGUMENT",
                        message = "target.type is required."
                    )
                )
            }

            when (targetType) {
                TARGET_TYPE_POINT -> {
                    val x = target.optIntOrNull("x")
                    val y = target.optIntOrNull("y")
                    if (x == null || y == null) {
                        return buildJsonResponse(
                            statusCode = 400,
                            body = errorEnvelope(
                                code = "INVALID_ARGUMENT",
                                message = "point target requires integer x and y."
                            )
                        )
                    }
                    stateCoordinator.tapPoint(x, y)
                }
                TARGET_TYPE_NODE -> {
                    val nodeId = target.optString("nodeId").trim()
                    if (nodeId.isEmpty()) {
                        return buildJsonResponse(
                            statusCode = 400,
                            body = errorEnvelope(
                                code = "INVALID_ARGUMENT",
                                message = "node target requires nodeId."
                            )
                        )
                    }
                    stateCoordinator.tapNode(nodeId)
                }
                else -> {
                    return buildJsonResponse(
                        statusCode = 422,
                        body = errorEnvelope(
                            code = "UNSUPPORTED_OPERATION",
                            message = "Unsupported tap target type: $targetType."
                        )
                    )
                }
            }
        }

        Log.i(
            TAP_LOG_TAG,
            "event=tap_request backendRequested=$backend backendUsed=${tapResult.backendUsed ?: "none"} tapPath=${tapResult.attemptedPath} success=${tapResult.performed} failure=${tapResult.failureReason ?: "none"}"
        )

        return when {
            tapResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", tapResult.backendUsed)
                )
            )
            tapResult.failureReason == TapFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for tap execution."
                )
            )
            tapResult.failureReason == TapFailureReason.NODE_NOT_FOUND -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "NODE_NOT_FOUND",
                    message = "Tap target node was not found."
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = "Accessibility tap action failed."
                )
            )
        }
    }

    private fun buildSwipeResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/swipe")
            ?: return badJsonBodyResponse()

        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> {
                return buildJsonResponse(
                    statusCode = 422,
                    body = errorEnvelope(
                        code = "UNSUPPORTED_OPERATION",
                        message = "Only backend=accessibility and backend=auto are supported."
                    )
                )
            }
        }

        val swipeCoordinates = parseSwipeCoordinates(body)
        if (!swipeCoordinates.valid) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = swipeCoordinates.errorMessage ?: "Swipe coordinates are invalid."
                )
            )
        }

        val durationMs = body.optLongOrNull("durationMs")
        if (durationMs == null || durationMs <= 0L || durationMs > MAX_SWIPE_DURATION_MS) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "durationMs must be between 1 and $MAX_SWIPE_DURATION_MS."
                )
            )
        }

        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val swipeResult = stateCoordinator.swipe(
            fromX = swipeCoordinates.fromX!!,
            fromY = swipeCoordinates.fromY!!,
            toX = swipeCoordinates.toX!!,
            toY = swipeCoordinates.toY!!,
            durationMs = durationMs
        )
        val observation = if (swipeResult.performed) {
            observeActionSurfaceChange(
                beforeSnapshot = beforeSnapshot,
                snapshotProvider = { stateCoordinator.getTreeSnapshotResult().snapshot }
            )
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }

        Log.i(
            SWIPE_LOG_TAG,
            "event=swipe_request backendRequested=$backend backendUsed=${swipeResult.backendUsed ?: "none"} swipePath=${swipeResult.attemptedPath} success=${swipeResult.performed} failure=${swipeResult.failureReason ?: "none"} from=${swipeCoordinates.fromX},${swipeCoordinates.fromY} to=${swipeCoordinates.toX},${swipeCoordinates.toY} durationMs=$durationMs requestShape=${swipeCoordinates.requestShape}"
        )

        return when {
            swipeResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    data = JSONObject()
                        .put("performed", true)
                        .put("backendUsed", swipeResult.backendUsed)
                        .put("requestShape", swipeCoordinates.requestShape)
                        .put("contentChanged", observation.surfaceChanged)
                        .put("beforeSnapshotToken", observation.beforeSnapshotToken)
                        .put("afterSnapshotToken", observation.afterSnapshotToken)
                        .put("finalPackageName", observation.finalPackageName)
                        .put("finalActivity", observation.finalActivity),
                    disclosure = buildMotionDisclosure(
                        route = "/swipe",
                        performed = swipeResult.performed,
                        surfaceChanged = observation.surfaceChanged
                    )
                )
            )
            swipeResult.failureReason == SwipeFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for swipe execution."
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = "Accessibility swipe action failed."
                )
            )
        }
    }

    private fun buildTypeResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/type")
            ?: return badJsonBodyResponse()

        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> {
                return buildJsonResponse(
                    statusCode = 422,
                    body = errorEnvelope(
                        code = "UNSUPPORTED_OPERATION",
                        message = "Only backend=accessibility and backend=auto are supported."
                    )
                )
            }
        }

        if (!body.has("text") || body.isNull("text")) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "text is required."
                )
            )
        }

        val rawText = body.opt("text")
        if (rawText !is String) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "text must be a string."
                )
            )
        }

        val typeResult = stateCoordinator.typeText(rawText)

        Log.i(
            TYPE_LOG_TAG,
            "event=type_request backendRequested=$backend backendUsed=${typeResult.backendUsed ?: "none"} typePath=${typeResult.attemptedPath} success=${typeResult.performed} failure=${typeResult.failureReason ?: "none"} textLength=${rawText.length}"
        )

        return when {
            typeResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", typeResult.backendUsed)
                )
            )
            typeResult.failureReason == TypeFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for text input."
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = if (typeResult.failureReason == TypeFailureReason.NO_EDITABLE_TARGET) {
                        "No focused editable target is available for text input."
                    } else {
                        "Accessibility text input action failed."
                    }
                )
            )
        }
    }

    private fun buildScreenResponse(queryParameters: Map<String, String>): String {
        val mode = ScreenReadMode.fromWireValue(queryParameters["source"]) ?: ScreenReadMode.ACCESSIBILITY
        val editableOnly = queryParameters["editable"] == "true"
        val scrollableOnly = queryParameters["scrollable"] == "true"
        val clickableOnly = queryParameters["clickable"] == "true"
        if (mode != ScreenReadMode.ACCESSIBILITY && (editableOnly || scrollableOnly || clickableOnly)) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "editable, scrollable, and clickable filters are only supported for source=accessibility."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (mode == ScreenReadMode.ACCESSIBILITY && !treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
        val payload = when (mode) {
            ScreenReadMode.ACCESSIBILITY -> {
                val requiredSnapshot = snapshot
                    ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)
                stateCoordinator.createScreenPayload(
                    snapshot = requiredSnapshot,
                    editableOnly = editableOnly,
                    scrollableOnly = scrollableOnly,
                    packageFilter = queryParameters["package"],
                    clickableOnly = clickableOnly
                )
            }
            ScreenReadMode.OCR -> GhosthandApiPayloads.screenReadPayload(
                stateCoordinator.createOcrScreenPayload()
            )
            ScreenReadMode.HYBRID -> {
                if (snapshot != null) {
                    GhosthandApiPayloads.screenReadPayload(
                        stateCoordinator.createHybridScreenPayload(
                            snapshot = snapshot,
                            packageFilter = queryParameters["package"]
                        )
                    )
                } else {
                    GhosthandApiPayloads.screenReadPayload(
                        stateCoordinator.createOcrScreenPayload()
                    )
                }
            }
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                data = payload,
                disclosure = buildScreenDisclosure(payload)
            )
        )
    }

    private fun buildScreenshotGetResponse(queryParameters: Map<String, String>): String {
        val width = queryParameters["width"]?.toIntOrNull() ?: 0
        val height = queryParameters["height"]?.toIntOrNull() ?: 0
        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)

        return if (screenshotResult.available) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }

    private fun buildScreenshotResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/screenshot")
            ?: return badJsonBodyResponse()

        val width = body.optIntOrNull("width") ?: 0
        val height = body.optIntOrNull("height") ?: 0

        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)

        return when {
            screenshotResult.available -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
            else -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }

    private fun buildInfoResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createInfoPayload())
        )
    }

    private fun buildFocusedResponse(): String {
        val result = stateCoordinator.getFocusedNodeResult()
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createFocusedNodePayload(result))
        )
    }

    private fun buildClickResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/click")
            ?: return badJsonBodyResponse()

        val nodeId = body.optString("nodeId").trim()
        val clickResult = if (nodeId.isNotEmpty()) {
            stateCoordinator.clickNode(nodeId)
        } else {
            val selector = parseSelector(body)
                ?: return buildJsonResponse(
                    statusCode = 400,
                    body = errorEnvelope(
                        code = "INVALID_ARGUMENT",
                        message = "nodeId or one of text, desc, id, or strategy is required."
                    )
                )

            stateCoordinator.clickFirstMatchFresh(
                strategy = selector.strategy,
                query = selector.query ?: "",
                clickableOnly = clickSelectorRequiresClickableTarget(body),
                index = body.optIntOrNull("index") ?: 0
            )
        }

        Log.i(
            CLICK_LOG_TAG,
            "event=click_request nodeId=$nodeId clickPath=${clickResult.attemptedPath} success=${clickResult.performed} failure=${clickResult.failureReason?.name ?: "none"} resolutionKind=${clickResult.selectorResolution?.resolutionKind ?: "none"} requestedStrategy=${clickResult.selectorResolution?.requestedStrategy ?: "none"} effectiveStrategy=${clickResult.selectorResolution?.effectiveStrategy ?: "none"}"
        )

        val selectorStrategy = if (nodeId.isEmpty()) parseSelector(body)?.strategy else null
        val clickableOnly = if (nodeId.isEmpty()) clickSelectorRequiresClickableTarget(body) else false

        return when {
            clickResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    data = GhosthandApiPayloads.clickPayload(clickResult),
                    disclosure = buildClickDisclosure(
                        strategy = selectorStrategy,
                        clickableOnly = clickableOnly,
                        result = clickResult
                    )
                )
            )
            clickResult.failureReason == ClickFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for click execution."
                )
            )
            clickResult.failureReason == ClickFailureReason.NODE_NOT_FOUND -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "NODE_NOT_FOUND",
                    message = "Click target node was not found.",
                    disclosure = buildClickDisclosure(
                        strategy = selectorStrategy,
                        clickableOnly = clickableOnly,
                        result = clickResult
                    )
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = "Accessibility click action failed."
                )
            )
        }
    }

    private fun buildInputResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/input")
            ?: return badJsonBodyResponse()

        val clear = body.optBoolean("clear", false)
        val append = body.optBoolean("append", false)
        val text = if (body.has("text") && !body.isNull("text")) body.opt("text") as? String else null

        if (!clear && text == null) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "text is required unless clear=true."
                )
            )
        }

        if (body.has("text") && !body.isNull("text") && text == null) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "text must be a string."
                )
            )
        }

        val typeResult = stateCoordinator.inputText(
            text = text,
            clear = clear,
            append = append
        )

        Log.i(
            INPUT_LOG_TAG,
            "event=input_request textLength=${text?.length ?: 0} action=${typeResult.action} inputPath=${typeResult.attemptedPath} success=${typeResult.performed} failure=${typeResult.failureReason?.name ?: "none"}"
        )

        return when {
            typeResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", typeResult.backendUsed)
                        .put("text", typeResult.finalText)
                        .put("previousText", typeResult.previousText)
                        .put("action", typeResult.action)
                )
            )
            typeResult.failureReason == TypeFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for text input."
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = if (typeResult.failureReason == TypeFailureReason.NO_EDITABLE_TARGET) {
                        "No focused editable target is available for text input."
                    } else {
                        "Accessibility text input action failed."
                    }
                )
            )
        }
    }

    private fun buildSetTextResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/setText")
            ?: return badJsonBodyResponse()

        val nodeId = body.optString("nodeId").trim()
        if (nodeId.isEmpty()) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "nodeId is required."
                )
            )
        }

        if (!body.has("text")) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "text is required."
                )
            )
        }

        val rawText = body.opt("text")
        val text = if (rawText is String) rawText else ""

        val setTextResult = stateCoordinator.setTextOnNode(nodeId, text)

        Log.i(
            SETTEXT_LOG_TAG,
            "event=settext_request nodeId=$nodeId settextPath=${setTextResult.attemptedPath} success=${setTextResult.performed} failure=${setTextResult.failureReason?.name ?: "none"}"
        )

        return when {
            setTextResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", setTextResult.backendUsed)
                )
            )
            setTextResult.failureReason == SetTextFailureReason.ACCESSIBILITY_UNAVAILABLE -> buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_UNAVAILABLE",
                    message = "Accessibility service is not available for text setting."
                )
            )
            setTextResult.failureReason == SetTextFailureReason.NODE_NOT_FOUND -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "NODE_NOT_FOUND",
                    message = "Target node was not found."
                )
            )
            setTextResult.failureReason == SetTextFailureReason.NODE_NOT_EDITABLE -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "NODE_NOT_EDITABLE",
                    message = "Target node is not editable or not enabled."
                )
            )
            else -> buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "ACCESSIBILITY_ACTION_FAILED",
                    message = "Set text action failed on the target node."
                )
            )
        }
    }

    private fun buildScrollResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/scroll")
            ?: return badJsonBodyResponse()

        val direction = body.optString("direction", "up").trim().lowercase()
        if (direction !in setOf("up", "down", "left", "right")) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "direction must be one of: up, down, left, right."))
        }

        val nodeId = body.optString("nodeId").trim().ifEmpty { null }
        val target = body.optString("target").trim().ifEmpty { null }
        val count = (body.optIntOrNull("count") ?: 1).coerceAtLeast(1)
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot

        val result = if (nodeId != null) {
            val single = stateCoordinator.scrollNode(nodeId, direction)
            ScrollBatchResult(
                performed = single.performed,
                performedCount = if (single.performed) 1 else 0,
                failureReason = single.failureReason,
                attemptedPath = single.attemptedPath
            )
        } else {
            stateCoordinator.scroll(
                direction = direction,
                target = target,
                count = count
            )
        }
        val observation = if (result.performed) {
            observeActionSurfaceChange(
                beforeSnapshot = beforeSnapshot,
                snapshotProvider = { stateCoordinator.getTreeSnapshotResult().snapshot }
            )
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }

        return when {
            result.performed -> buildJsonResponse(
                200,
                successEnvelope(
                    data = JSONObject()
                        .put("performed", true)
                        .put("count", result.performedCount)
                        .put("direction", direction)
                        .put("attemptedPath", result.attemptedPath)
                        .put("contentChanged", observation.surfaceChanged)
                        .put("surfaceChanged", observation.surfaceChanged)
                        .put("beforeSnapshotToken", observation.beforeSnapshotToken)
                        .put("afterSnapshotToken", observation.afterSnapshotToken)
                        .put("finalPackageName", observation.finalPackageName)
                        .put("finalActivity", observation.finalActivity),
                    disclosure = buildMotionDisclosure(
                        route = "/scroll",
                        performed = result.performed,
                        surfaceChanged = observation.surfaceChanged
                    )
                )
            )
            result.failureReason == ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available."))
            result.failureReason == ScrollFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Scroll target node was not found."))
            result.failureReason == ScrollFailureReason.INVALID_DIRECTION ->
                buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "Invalid scroll direction."))
            else ->
                buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Scroll gesture failed."))
        }
    }

    private fun buildLongpressResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/longpress")
            ?: return badJsonBodyResponse()

        val x = body.optIntOrNull("x")
        val y = body.optIntOrNull("y")
        if (x == null || y == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "x and y are required."))
        }

        val durationMs = body.optLongOrNull("durationMs") ?: 500L
        if (durationMs < 100 || durationMs > 10000) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "durationMs must be between 100 and 10000."))
        }

        val performed = stateCoordinator.performLongPressGesture(x, y, durationMs)
        return if (performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("performed", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Long-press gesture failed."))
        }
    }

    private fun buildGestureResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/gesture")
            ?: return badJsonBodyResponse()

        val type = body.optString("type").trim().lowercase()
        if (type == "pinch_in" || type == "pinch_out") {
            val x = body.optIntOrNull("x") ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "x is required."))
            val y = body.optIntOrNull("y") ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "y is required."))
            val distance = body.optIntOrNull("distance") ?: 200
            val durationMs = body.optLongOrNull("durationMs") ?: 300L
            val half = (distance / 2).coerceAtLeast(20)
            val strokes = if (type == "pinch_in") {
                listOf(
                    GestureStroke(listOf(GesturePoint(x - half, y), GesturePoint(x, y)), durationMs),
                    GestureStroke(listOf(GesturePoint(x + half, y), GesturePoint(x, y)), durationMs)
                )
            } else {
                listOf(
                    GestureStroke(listOf(GesturePoint(x, y), GesturePoint(x - half, y)), durationMs),
                    GestureStroke(listOf(GesturePoint(x, y), GesturePoint(x + half, y)), durationMs)
                )
            }
            val performed = stateCoordinator.performGesture(strokes)
            return if (performed) {
                buildJsonResponse(200, successEnvelope(JSONObject().put("performed", true)))
            } else {
                buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Gesture dispatch failed."))
            }
        }

        val strokesJson = body.optJSONArray("strokes")
        if (strokesJson == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "strokes is required."))
        }

        val strokes = mutableListOf<GestureStroke>()
        for (i in 0 until strokesJson.length()) {
            val strokeJson = strokesJson.optJSONObject(i) ?: continue
            val pointsJson = strokeJson.optJSONArray("points")
            if (pointsJson == null || pointsJson.length() == 0) continue

            val points = mutableListOf<GesturePoint>()
            for (j in 0 until pointsJson.length()) {
                val pt = pointsJson.optJSONObject(j) ?: continue
                val px = pt.optIntOrNull("x") ?: continue
                val py = pt.optIntOrNull("y") ?: continue
                points.add(GesturePoint(px, py))
            }
            if (points.isNotEmpty()) {
                val durationMs = strokeJson.optLongOrNull("durationMs") ?: 300L
                strokes.add(GestureStroke(points, durationMs))
            }
        }

        if (strokes.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "At least one valid stroke with points is required."))
        }

        val performed = stateCoordinator.performGesture(strokes)
        return if (performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("performed", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Gesture dispatch failed."))
        }
    }

    private fun buildGlobalActionResponse(actionName: String, actionCode: Int): String {
        val result = stateCoordinator.performGlobalAction(actionCode)
        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("performed", true)))
        } else {
            buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Global action '$actionName' failed. Accessibility service may not be connected."))
        }
    }

    private fun buildLaunchResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/launch")
            ?: return badJsonBodyResponse()
        val packageName = body.optString("packageName").trim()
        if (packageName.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "packageName is required."))
        }

        val result = stateCoordinator.launchApp(packageName)
        val details = JSONObject()
            .put("launched", result.launched)
            .put("packageName", result.packageName)
            .put("label", result.label ?: JSONObject.NULL)
            .put("strategy", result.strategy)
            .put("reason", result.reason)

        return when (result.reason) {
            "launched" -> buildJsonResponse(200, successEnvelope(details))
            "package_not_installed" -> buildJsonResponse(
                404,
                errorEnvelope(
                    "PACKAGE_NOT_FOUND",
                    "Package is not installed: ${result.packageName}.",
                    details
                )
            )
            "launch_intent_unavailable" -> buildJsonResponse(
                422,
                errorEnvelope(
                    "NO_LAUNCH_INTENT",
                    "Package is installed but does not expose a standard launch intent: ${result.packageName}.",
                    details
                )
            )
            else -> buildJsonResponse(
                503,
                errorEnvelope(
                    "LAUNCH_FAILED",
                    "Launch attempt failed for package ${result.packageName}.",
                    details.put("error", result.error ?: JSONObject.NULL)
                )
            )
        }
    }

    private fun buildClipboardReadResponse(): String {
        val result = stateCoordinator.readClipboard()
        return if (result.available) {
            val payload = JSONObject()
                .put("text", result.text ?: JSONObject.NULL)
            if (result.attemptedPath == "clipboard_cached_after_write") {
                payload.put("reason", result.attemptedPath)
            }
            buildJsonResponse(200, successEnvelope(payload))
        } else {
            buildJsonResponse(200, successEnvelope(JSONObject()
                .put("text", JSONObject.NULL)
                .put("reason", result.attemptedPath)))
        }
    }

    private fun buildClipboardWriteResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/clipboard")
            ?: return badJsonBodyResponse()

        if (!body.has("text")) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }

        val rawText = body.opt("text")
        val text = if (rawText is String) rawText else rawText?.toString() ?: ""
        val result = stateCoordinator.writeClipboard(text)

        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("written", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("CLIPBOARD_WRITE_FAILED", "Clipboard write failed."))
        }
    }

    private fun buildWaitUiChangeResponse(queryParameters: Map<String, String>): String {
        val timeoutMs = queryParameters["timeout"]?.toLongOrNull()
            ?: queryParameters["timeoutMs"]?.toLongOrNull()
            ?: 5000L
        val intervalMs = queryParameters["intervalMs"]?.toLongOrNull() ?: 200L
        val result = stateCoordinator.waitForUiChange(timeoutMs, intervalMs)
        return buildJsonResponse(
            200,
            successEnvelope(
                data = JSONObject()
                    .put("changed", result.changed)
                    .put("conditionMet", JSONObject.NULL)
                    .put("stateChanged", result.outcome.stateChanged)
                    .put("timedOut", result.outcome.timedOut)
                    .put("elapsedMs", result.elapsedMs)
                    .put("snapshotToken", result.snapshotToken ?: JSONObject.NULL)
                    .put("packageName", result.packageName ?: JSONObject.NULL)
                    .put("activity", result.activity ?: JSONObject.NULL),
                disclosure = buildWaitUiChangeDisclosure(result)
            )
        )
    }

    private fun buildWaitResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/wait")
            ?: return badJsonBodyResponse()

        val condition = body.optJSONObject("condition")
            ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "condition is required."))

        val selector = parseSelector(condition)
        if (selector == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "condition.strategy is required."))
        }
        val strategy = selector.strategy

        if (strategy !in SUPPORTED_WAIT_STRATEGIES) {
            return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Unsupported /wait strategy: $strategy."))
        }

        if (selector.query == null && strategy != "focused") {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "condition query is required for strategy: $strategy."))
        }
        val query = selector.query

        val timeoutMs = body.optLongOrNull("timeoutMs") ?: 5000L
        if (timeoutMs < 0) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "timeoutMs must be non-negative."))
        }

        val intervalMs = body.optLongOrNull("intervalMs") ?: 200L
        if (intervalMs < 50) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "intervalMs must be at least 50."))
        }

        val result = stateCoordinator.waitForCondition(strategy, query, timeoutMs, intervalMs)
        val normalized = normalizeWaitConditionResult(result)

        return if (normalized.satisfied) {
            buildJsonResponse(200, successEnvelope(
                data = JSONObject()
                    .put("satisfied", true)
                    .put("conditionMet", normalized.conditionMet)
                    .put("stateChanged", normalized.stateChanged)
                    .put("timedOut", normalized.timedOut)
                    .put("elapsedMs", result.elapsedMs)
                    .put("node", normalized.node?.let { node ->
                        JSONObject()
                            .put("nodeId", node.nodeId)
                            .put("text", node.text ?: JSONObject.NULL)
                            .put("contentDesc", node.contentDesc ?: JSONObject.NULL)
                            .put("resourceId", node.resourceId ?: JSONObject.NULL)
                    } ?: JSONObject.NULL)
            ))
        } else {
            buildJsonResponse(200, successEnvelope(
                data = JSONObject()
                    .put("satisfied", false)
                    .put("conditionMet", normalized.conditionMet)
                    .put("stateChanged", normalized.stateChanged)
                    .put("timedOut", normalized.timedOut)
                    .put("elapsedMs", result.elapsedMs)
                    .put("reason", normalized.reason),
                disclosure = buildWaitConditionDisclosure(strategy, normalized)
            ))
        }
    }

    private fun buildNotifyReadResponse(queryParameters: Map<String, String>): String {
        val excludedPackages = queryParameters["exclude"]
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toSet()
            ?: emptySet()
        return buildJsonResponse(
            200,
            successEnvelope(
                stateCoordinator.readNotifications(
                    packageFilter = queryParameters["package"]?.ifBlank { null },
                    excludedPackages = excludedPackages
                )
            )
        )
    }

    private fun buildNotifyPostResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/notify")
            ?: return badJsonBodyResponse()
        val title = body.optString("title", "").trim()
        val text = body.optString("text", "").trim()
        if (text.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }
        val result = stateCoordinator.postNotification(title, text)
        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject()
                .put("posted", true)
                .put("notificationId", result.notificationId ?: JSONObject.NULL)))
        } else {
            buildJsonResponse(503, errorEnvelope("NOTIFICATION_FAILED", "Failed to post notification. Reason: ${result.attemptedPath}"))
        }
    }

    private fun buildNotifyCancelResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/notify")
            ?: return badJsonBodyResponse()
        val notificationId = body.optIntOrNull("notificationId")
        if (notificationId == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "notificationId is required."))
        }
        val result = stateCoordinator.cancelNotification(notificationId)
        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("canceled", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("NOTIFICATION_CANCEL_FAILED", "Failed to cancel notification."))
        }
    }

    private fun successEnvelope(data: JSONObject, disclosure: GhosthandDisclosure? = null): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put("data", data)
            .apply {
                disclosure?.let { put("disclosure", GhosthandApiPayloads.disclosureJson(it)) }
            }
            .put("meta", buildMeta())
    }

    private fun errorEnvelope(
        code: String,
        message: String,
        details: JSONObject = JSONObject(),
        disclosure: GhosthandDisclosure? = null
    ): JSONObject {
        return JSONObject()
            .put("ok", false)
            .put("error", JSONObject()
                .put("code", code)
                .put("message", message)
                .put("details", details)
            )
            .apply {
                disclosure?.let { put("disclosure", GhosthandApiPayloads.disclosureJson(it)) }
            }
            .put("meta", buildMeta())
    }

    private fun buildMeta(): JSONObject {
        return JSONObject()
            .put("requestId", "req_${UUID.randomUUID().toString().replace("-", "")}")
            .put("timestamp", Instant.now().toString())
    }

    private fun buildJsonResponse(statusCode: Int, body: JSONObject): String {
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

    private fun parseSelector(body: JSONObject): SelectorQuery? {
        val query = body.opt("query").let { value ->
            when {
                value == null || value == JSONObject.NULL -> null
                else -> value.toString()
            }
        }

        return GhosthandSelectors.normalize(
            text = body.optString("text").ifBlank { null },
            desc = body.optString("desc").ifBlank { null },
            id = body.optString("id").ifBlank { null },
            strategy = body.optString("strategy").ifBlank { null },
            query = query
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        val value = opt(key)
        return if (value is Number) value.toInt() else null
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        val value = opt(key)
        return if (value is Number) value.toLong() else null
    }

    private fun parseJsonBodyOrNull(requestBody: String, endpoint: String): JSONObject? {
        return try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (error: JSONException) {
            Log.w(
                LOG_TAG,
                "component=LocalApiServer operation=parseJsonBody endpoint=$endpoint failure=${error.javaClass.simpleName}",
                error
            )
            null
        }
    }

    private fun badJsonBodyResponse(): String {
        return buildJsonResponse(
            400,
            errorEnvelope("BAD_REQUEST", "Request body must be valid JSON.")
        )
    }

    private fun createResources(): LocalApiServerResources {
        return LocalApiServerResources(
            serverExecutor = Executors.newSingleThreadExecutor(serverThreadFactory("ghosthand-local-api")),
            clientExecutor = ThreadPoolExecutor(
                CLIENT_POOL_SIZE,
                CLIENT_POOL_SIZE,
                0L,
                TimeUnit.MILLISECONDS,
                ArrayBlockingQueue(CLIENT_QUEUE_CAPACITY),
                serverThreadFactory("ghosthand-local-client"),
                ThreadPoolExecutor.AbortPolicy()
            )
        )
    }

    private fun serverThreadFactory(namePrefix: String): ThreadFactory {
        val count = AtomicInteger(1)
        return ThreadFactory { runnable ->
            Thread(runnable, "$namePrefix-${count.getAndIncrement()}")
        }
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 5583
        const val LOG_TAG = "LocalApiServer"
        const val MAX_REQUEST_LINE_BYTES = 4096
        const val MAX_HEADER_LINE_BYTES = 8192
        const val MAX_HEADER_COUNT = 40
        const val MAX_HEADER_BYTES = 16 * 1024
        const val MAX_BODY_BYTES = 256 * 1024
        const val CLIENT_READ_TIMEOUT_MS = 5_000
        const val CLIENT_POOL_SIZE = 4
        const val CLIENT_QUEUE_CAPACITY = 16
        const val SHUTDOWN_TIMEOUT_MS = 2_000L
        private const val TREE_MODE_RAW = "raw"
        private const val TREE_MODE_FLAT = "flat"
        private const val DEFAULT_TREE_MODE = TREE_MODE_RAW
        private const val FIND_STRATEGY_FOCUSED = "focused"
        private const val TARGET_TYPE_POINT = "point"
        private const val TARGET_TYPE_NODE = "node"
        private const val BACKEND_AUTO = "auto"
        private const val BACKEND_ACCESSIBILITY = "accessibility"
        private const val DEFAULT_BACKEND = "auto"
        private const val TAP_LOG_TAG = "GhostTap"
        private const val SWIPE_LOG_TAG = "GhostSwipe"
        private const val TYPE_LOG_TAG = "GhostType"
        private const val CLICK_LOG_TAG = "GhostClick"
        private const val INPUT_LOG_TAG = "GhostInput"
        private const val SETTEXT_LOG_TAG = "GhostSetText"
        private const val SCROLL_LOG_TAG = "GhostScroll"
        private const val LONGPRESS_LOG_TAG = "GhostLongpress"
        private const val GESTURE_LOG_TAG = "GhostGesture"
        private const val MAX_SWIPE_DURATION_MS = 5000L
        private const val ACTION_SETTLE_DELAY_MS = 300L
        private val SUPPORTED_FIND_STRATEGIES = setOf(
            "text",
            "textContains",
            "resourceId",
            "contentDesc",
            "contentDescContains",
            FIND_STRATEGY_FOCUSED
        )
        private val SUPPORTED_WAIT_STRATEGIES = setOf(
            "text",
            "textContains",
            "resourceId",
            "contentDesc",
            "contentDescContains",
            "focused"
        )
    }
}

internal object CapabilityRoutePolicy {
    internal val accessibilityPaths = setOf(
        "/tree",
        "/find",
        "/tap",
        "/swipe",
        "/type",
        "/screen",
        "/focused",
        "/click",
        "/input",
        "/setText",
        "/scroll",
        "/longpress",
        "/gesture",
        "/back",
        "/home",
        "/recents",
        "/wait"
    )
    private val screenshotPaths = setOf("/screenshot")
    fun routeCapability(path: String): GhosthandCapability? {
        return when {
            path in accessibilityPaths -> GhosthandCapability.Accessibility
            path in screenshotPaths -> GhosthandCapability.Screenshot
            else -> null
        }
    }

    fun policyDeniedResponse(
        path: String,
        capabilityAccess: CapabilityAccessSnapshot
    ): String? {
        val capability = routeCapability(path) ?: return null
        val gateState = capabilityAccess.gateStateFor(capability)
        if (gateState.policyAllowed) {
            return null
        }
        return denialMessage(capability)
    }

    fun denialMessage(capability: GhosthandCapability): String {
        return when (capability) {
            GhosthandCapability.Accessibility ->
                "Accessibility control is disabled by app policy. Enable it on the Permissions page before using accessibility-backed Ghosthand routes."
            GhosthandCapability.Screenshot ->
                "Screenshot capture is disabled by app policy. Enable it on the Permissions page before using screenshot routes."
        }
    }
}

internal data class ParsedSwipeCoordinates(
    val valid: Boolean,
    val fromX: Int? = null,
    val fromY: Int? = null,
    val toX: Int? = null,
    val toY: Int? = null,
    val requestShape: String? = null,
    val errorMessage: String? = null
)

internal fun parseSwipeCoordinates(body: JSONObject): ParsedSwipeCoordinates {
    val from = body.optJSONObject("from")
    val to = body.optJSONObject("to")
    if (from != null || to != null) {
        if (from == null || to == null) {
            return ParsedSwipeCoordinates(valid = false, errorMessage = "from and to are both required.")
        }
        val fromX = jsonOptIntOrNull(from, "x")
        val fromY = jsonOptIntOrNull(from, "y")
        val toX = jsonOptIntOrNull(to, "x")
        val toY = jsonOptIntOrNull(to, "y")
        return if (fromX == null || fromY == null || toX == null || toY == null) {
            ParsedSwipeCoordinates(valid = false, errorMessage = "from/to must contain integer x and y values.")
        } else {
            ParsedSwipeCoordinates(
                valid = true,
                fromX = fromX,
                fromY = fromY,
                toX = toX,
                toY = toY,
                requestShape = "from_to"
            )
        }
    }

    val x1 = jsonOptIntOrNull(body, "x1")
    val y1 = jsonOptIntOrNull(body, "y1")
    val x2 = jsonOptIntOrNull(body, "x2")
    val y2 = jsonOptIntOrNull(body, "y2")
    return if (x1 != null && y1 != null && x2 != null && y2 != null) {
        ParsedSwipeCoordinates(
            valid = true,
            fromX = x1,
            fromY = y1,
            toX = x2,
            toY = y2,
            requestShape = "xy_alias"
        )
    } else {
        ParsedSwipeCoordinates(
            valid = false,
            errorMessage = "Use canonical from/to point objects or the x1/y1/x2/y2 alias form."
        )
    }
}

internal fun clickSelectorRequiresClickableTarget(body: JSONObject): Boolean {
    return if (body.has("clickable")) {
        body.optBoolean("clickable", false)
    } else {
        true
    }
}

internal data class ScrollSurfaceObservation(
    val surfaceChanged: Boolean,
    val beforeSnapshotToken: String?,
    val afterSnapshotToken: String?,
    val finalPackageName: String?,
    val finalActivity: String?
)

internal fun observeScrollSurfaceChange(
    beforeSnapshot: AccessibilityTreeSnapshot?,
    afterSnapshot: AccessibilityTreeSnapshot?
): ScrollSurfaceObservation {
    val beforeToken = beforeSnapshot?.snapshotToken
    val afterToken = afterSnapshot?.snapshotToken
    val beforePackage = beforeSnapshot?.packageName
    val afterPackage = afterSnapshot?.packageName
    val beforeActivity = beforeSnapshot?.activity
    val afterActivity = afterSnapshot?.activity
    val surfaceChanged =
        (beforeToken != null && afterToken != null && beforeToken != afterToken) ||
            (beforePackage != afterPackage) ||
            (beforeActivity != afterActivity)

    return ScrollSurfaceObservation(
        surfaceChanged = surfaceChanged,
        beforeSnapshotToken = beforeToken,
        afterSnapshotToken = afterToken,
        finalPackageName = afterPackage,
        finalActivity = afterActivity
    )
}

internal fun observeActionSurfaceChange(
    beforeSnapshot: AccessibilityTreeSnapshot?,
    snapshotProvider: () -> AccessibilityTreeSnapshot?
): ScrollSurfaceObservation {
    Thread.sleep(300L)
    val afterSnapshot = snapshotProvider()
    return observeScrollSurfaceChange(beforeSnapshot, afterSnapshot)
}

internal fun buildWaitUiChangeDisclosure(
    result: StateCoordinator.WaitUiChangeResult
): GhosthandDisclosure? {
    if (result.changed) {
        return null
    }
    return GhosthandDisclosure(
        kind = "discoverability",
        summary = "GET /wait reports whether a transition was observed during the wait window, not whether the current screen is unusable.",
        assumptionToCorrect = "`changed=false` means the action failed.",
        nextBestActions = listOf(
            "Use /screen to inspect the final settled surface.",
            "Use /tree if you need fuller structural truth."
        )
    )
}

internal fun buildWaitConditionDisclosure(
    strategy: String,
    result: NormalizedWaitConditionResult
): GhosthandDisclosure? {
    if (result.satisfied) {
        return null
    }
    return GhosthandDisclosure(
        kind = "discoverability",
        summary = "POST /wait only waits for a matching selector condition; it is not the generic settle-wait route.",
        assumptionToCorrect = "POST /wait behaves like GET /wait.",
        nextBestActions = listOf(
            "Use GET /wait when you need a settle window after an action.",
            "Use /find with ${selectorAliasForStrategy(strategy)} when you need to inspect selector availability first."
        )
    )
}

internal data class NormalizedWaitConditionResult(
    val satisfied: Boolean,
    val conditionMet: Boolean,
    val stateChanged: Boolean,
    val timedOut: Boolean,
    val node: FlatAccessibilityNode?,
    val reason: String
)

internal fun normalizeWaitConditionResult(
    result: StateCoordinator.WaitConditionResult
): NormalizedWaitConditionResult {
    val normalizedSatisfied = result.matchedCondition()
    val normalizedTimedOut = if (normalizedSatisfied) {
        false
    } else {
        result.outcome.timedOut ||
            result.attemptedPath == "timeout" ||
            result.satisfied ||
            result.outcome.conditionMet == true
    }

    return NormalizedWaitConditionResult(
        satisfied = normalizedSatisfied,
        conditionMet = normalizedSatisfied,
        stateChanged = result.outcome.stateChanged,
        timedOut = normalizedTimedOut,
        node = if (normalizedSatisfied) result.node else null,
        reason = if (normalizedSatisfied) {
            "condition_met"
        } else if (normalizedTimedOut) {
            "timeout"
        } else {
            result.attemptedPath
        }
    )
}

internal fun buildFindDisclosure(
    strategy: String,
    clickableOnly: Boolean,
    result: FindNodeResult
): GhosthandDisclosure? {
    if (result.found) {
        return null
    }
    val missHint = result.missHint
    val searchedSurface = missHint?.searchedSurface ?: selectorAliasForStrategy(strategy)
    val matchSemantics = missHint?.matchSemantics ?: selectorMatchSemantics(strategy)
    if (clickableOnly) {
        return GhosthandDisclosure(
            kind = "discoverability",
            summary = "This $matchSemantics $searchedSurface lookup only returned actionable targets because `clickable=true` was enabled.",
            assumptionToCorrect = "A visible label must itself be directly clickable to be discoverable.",
            nextBestActions = listOf(
                "Retry /find without clickable=true to inspect child labels first.",
                findAlternateAction(strategy, missHint)
            )
        )
    }
    val summary = when (missHint?.likelyMissReason) {
        "visible_text_is_part_of_a_longer_text_block" ->
            "This lookup used exact text matching, so a visible prefix can still miss when the real node text is longer."
        "visible_desc_is_part_of_a_longer_content_description" ->
            "This lookup used exact content-description matching, so a visible prefix can still miss when the real description is longer."
        "meaningful_label_may_live_in_content_description" ->
            "This lookup searched exact text only; visible content can instead live in content descriptions."
        "meaningful_label_may_live_in_text" ->
            "This lookup searched $matchSemantics content descriptions only; the meaningful label can instead live in text."
        "visible_label_is_not_the_same_as_a_resource_id" ->
            "This lookup searched exact resource ids only; a visible label is not the same thing as a resource id."
        "resource_id_may_be_easier_to_target_than_visible_text" ->
            "This lookup searched exact text only; the element may be easier to target by resource id."
        else ->
            "This lookup searched the $searchedSurface surface using $matchSemantics matching only."
    }
    val assumptionToCorrect = when (missHint?.likelyMissReason) {
        "visible_text_is_part_of_a_longer_text_block",
        "visible_desc_is_part_of_a_longer_content_description" ->
            "/screen-visible text always matches exact /find text."
        "meaningful_label_may_live_in_content_description",
        "meaningful_label_may_live_in_text" ->
            "The visible label always lives on the selector surface I just searched."
        "visible_label_is_not_the_same_as_a_resource_id" ->
            "A visible label should be discoverable as an exact resource id."
        else ->
            "A /find miss means the platform cannot see the node."
    }
    val nextActions = mutableListOf<String>()
    missHint?.suggestedAlternateStrategies?.firstOrNull()?.let { nextActions += "Retry with $it." }
    if (nextActions.size < 2) {
        nextActions += when {
            missHint?.suggestedAlternateSurfaces?.contains("contentDesc") == true ->
                "Inspect /screen for desc or retry with contentDesc."
            missHint?.suggestedAlternateSurfaces?.contains("text") == true ->
                "Inspect /screen for text or retry with text."
            missHint?.suggestedAlternateSurfaces?.contains("resourceId") == true ->
                "Inspect /screen for id or retry with resourceId."
            else ->
                findAlternateAction(strategy, missHint)
        }
    }
    return GhosthandDisclosure(
        kind = "discoverability",
        summary = summary,
        assumptionToCorrect = assumptionToCorrect,
        nextBestActions = nextActions.distinct().take(2)
    )
}

internal fun buildClickDisclosure(
    strategy: String?,
    clickableOnly: Boolean,
    result: ClickAttemptResult
): GhosthandDisclosure? {
    val resolution = result.selectorResolution
    if (result.performed && resolution != null) {
        if (resolution.usedContainsFallback || resolution.resolutionKind != "matched_node") {
            val summary = when {
                resolution.usedSurfaceFallback && resolution.usedContainsFallback && resolution.resolutionKind == "clickable_ancestor" ->
                    "Ghosthand crossed from the requested selector surface to a bounded contains match on another surface, then dispatched the click on a clickable ancestor."
                resolution.usedSurfaceFallback && resolution.resolutionKind == "clickable_ancestor" ->
                    "Ghosthand matched the label on a different selector surface and dispatched the click on its clickable ancestor."
                resolution.usedSurfaceFallback && resolution.usedContainsFallback ->
                    "Ghosthand crossed to a bounded contains match on another selector surface before clicking."
                resolution.usedSurfaceFallback ->
                    "Ghosthand matched the label on a different selector surface before dispatching the click."
                resolution.usedContainsFallback && resolution.resolutionKind == "clickable_ancestor" ->
                    "Ghosthand widened the selector match and dispatched the click on a clickable ancestor."
                resolution.resolutionKind == "clickable_ancestor" ->
                    "Ghosthand matched a child label and dispatched the click on its clickable ancestor."
                resolution.usedContainsFallback ->
                    "Ghosthand widened the selector match with a bounded contains fallback before clicking."
                else ->
                    "Ghosthand used bounded selector reconciliation before dispatching the click."
            }
            return GhosthandDisclosure(
                kind = "fallback",
                summary = summary,
                assumptionToCorrect = if (resolution.usedSurfaceFallback) {
                    "The meaningful label must live on the exact selector surface I requested."
                } else {
                    "The matched visible label is always the directly clickable node."
                },
                nextBestActions = listOf(
                    "Use /find if you need to inspect the matched node before clicking.",
                    alternateSelectorAction(strategy ?: resolution.requestedStrategy)
                )
            )
        }
        return null
    }

    if (!result.performed && result.failureReason == ClickFailureReason.NODE_NOT_FOUND && strategy != null) {
        return GhosthandDisclosure(
            kind = "discoverability",
            summary = if (clickableOnly) {
                "Selector-based click only searched for actionable targets on the requested selector surface."
            } else {
                "Selector-based click only searched the requested selector surface."
            },
            assumptionToCorrect = if (clickableOnly) {
                "The visible label is always directly actionable on the same node."
            } else {
                "The requested selector surface is the only place the label can live."
            },
            nextBestActions = listOf(
                if (clickableOnly) {
                    "Use /find without clickable=true to inspect the matched node first."
                } else {
                    "Use /find to inspect whether the label is present on a different surface."
                },
                alternateSelectorAction(strategy)
            )
        )
    }
    return null
}

internal fun buildScreenDisclosure(payload: JSONObject): GhosthandDisclosure? {
    return buildScreenDisclosure(
        partialOutput = payload.optBoolean("partialOutput", false),
        foregroundStableDuringCapture = payload.optBoolean("foregroundStableDuringCapture", true)
    )
}

internal fun buildScreenDisclosure(
    partialOutput: Boolean,
    foregroundStableDuringCapture: Boolean
): GhosthandDisclosure? {
    val unstableCapture = !foregroundStableDuringCapture
    if (!partialOutput && !unstableCapture) {
        return null
    }
    return GhosthandDisclosure(
        kind = if (partialOutput) "shaped_output" else "constraint",
        summary = if (partialOutput) {
            "This /screen result is a reduced actionable view; omitted elements are not proof of absence."
        } else {
            "This /screen capture completed under foreground drift, so absence should be treated cautiously."
        },
        assumptionToCorrect = "Anything not returned by /screen was not present.",
        nextBestActions = listOf(
            "Use /tree when you need fuller structural truth.",
            "Use /screenshot when visual truth and structured output disagree."
        )
    )
}

internal fun buildMotionDisclosure(
    route: String,
    performed: Boolean,
    surfaceChanged: Boolean
): GhosthandDisclosure? {
    if (!performed || surfaceChanged) {
        return null
    }
    return GhosthandDisclosure(
        kind = "ambiguity",
        summary = "$route dispatched successfully, but Ghosthand did not observe visible-state change yet.",
        assumptionToCorrect = "`performed=true` proves the content advanced.",
        nextBestActions = listOf(
            "Use GET /wait to allow the surface to settle.",
            "Use /screen to confirm whether visible content actually changed."
        )
    )
}

private fun selectorAliasForStrategy(strategy: String): String {
    return when (strategy) {
        "contentDesc", "contentDescContains" -> "desc"
        "resourceId" -> "id"
        "focused" -> "focused"
        else -> "text"
    }
}

private fun alternateSelectorAction(strategy: String): String {
    return when (strategy) {
        "text", "textContains" -> "Retry with desc if the meaningful label lives in content descriptions."
        "contentDesc", "contentDescContains" -> "Retry with text if the visible label is rendered as text."
        "resourceId" -> "Retry with text or desc if you only know the visible label."
        else -> "Retry with text, desc, or id based on the surface you can actually observe."
    }
}

private fun findAlternateAction(strategy: String, missHint: FindMissHint?): String {
    return when {
        missHint?.suggestedAlternateSurfaces?.contains("contentDesc") == true ->
            "Retry with contentDesc if the meaningful label lives in desc."
        missHint?.suggestedAlternateSurfaces?.contains("text") == true ->
            "Retry with text if the meaningful label lives in visible text."
        missHint?.suggestedAlternateSurfaces?.contains("resourceId") == true ->
            "Retry with resourceId if the element is easier to target by id."
        else -> alternateSelectorAction(strategy)
    }
}

private fun selectorMatchSemantics(strategy: String): String {
    return when (strategy) {
        "textContains", "contentDescContains" -> "contains"
        "focused" -> "state"
        else -> "exact"
    }
}

private fun jsonOptIntOrNull(json: JSONObject, key: String): Int? {
    val value = json.opt(key)
    return if (value is Number) value.toInt() else null
}
