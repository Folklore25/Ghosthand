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
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
    private val serverExecutor = Executors.newSingleThreadExecutor()
    private val clientExecutor = Executors.newCachedThreadPool()

    @Volatile
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        serverExecutor.execute {
            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(InetAddress.getByName(HOST), PORT))
                }
                serverSocket = socket
                RuntimeStateStore.markLocalApiServerStarted()
                Log.i(LOG_TAG, "Listening on $HOST:$PORT")

                while (running.get()) {
                    val client = socket.accept()
                    clientExecutor.execute {
                        handleClient(client)
                    }
                }
            } catch (error: SocketException) {
                if (running.get()) {
                    RuntimeStateStore.markLocalApiServerFailed(error.message ?: "socket error")
                    Log.e(LOG_TAG, "Socket failure", error)
                }
            } catch (error: Exception) {
                RuntimeStateStore.markLocalApiServerFailed(error.message ?: "unknown error")
                Log.e(LOG_TAG, "Startup failure", error)
            } finally {
                closeServerSocket()
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
        closeServerSocket()
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                val inputStream = client.getInputStream()
                val requestLine = GhosthandHttp.readHttpLine(inputStream) ?: return
                val headers = mutableMapOf<String, String>()

                while (true) {
                    val headerLine = GhosthandHttp.readHttpLine(inputStream) ?: break
                    if (headerLine.isBlank()) {
                        break
                    }
                    val separatorIndex = headerLine.indexOf(':')
                    if (separatorIndex <= 0) {
                        continue
                    }

                    headers[headerLine.substring(0, separatorIndex).trim().lowercase()] =
                        headerLine.substring(separatorIndex + 1).trim()
                }

                val requestParts = requestLine.split(" ")
                val method = requestParts.getOrNull(0).orEmpty()
                val requestTarget = requestParts.getOrNull(1).orEmpty()
                val target = GhosthandHttp.parseRequestTarget(requestTarget)
                val path = target.path
                val queryParameters = target.queryParameters
                val requestBody = GhosthandHttp.readUtf8Body(inputStream, headers["content-length"])

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
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Request handling failure", error)
                OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
                    writer.write(
                        buildJsonResponse(
                            statusCode = 500,
                            body = errorEnvelope(
                                code = "INTERNAL_ERROR",
                                message = "Failed to handle request."
                            )
                        )
                    )
                    writer.flush()
                }
            }
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                stateCoordinator.createFindPayload(
                    snapshot = treeSnapshotResult.snapshot
                        ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT),
                    strategy = selector.strategy,
                    query = selector.query,
                    clickableOnly = body.optBoolean("clickable", false),
                    index = body.optIntOrNull("index") ?: 0
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", swipeResult.backendUsed)
                        .put("requestShape", swipeCoordinates.requestShape)
                        .put("contentChanged", observation.surfaceChanged)
                        .put("beforeSnapshotToken", observation.beforeSnapshotToken)
                        .put("afterSnapshotToken", observation.afterSnapshotToken)
                        .put("finalPackageName", observation.finalPackageName)
                        .put("finalActivity", observation.finalActivity)
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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
        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
            ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                stateCoordinator.createScreenPayload(
                    snapshot = snapshot,
                    editableOnly = queryParameters["editable"] == "true",
                    scrollableOnly = queryParameters["scrollable"] == "true",
                    packageFilter = queryParameters["package"],
                    clickableOnly = queryParameters["clickable"] == "true"
                )
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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

        return when {
            clickResult.performed -> buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(GhosthandApiPayloads.clickPayload(clickResult))
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
                    message = "Click target node was not found."
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "BAD_REQUEST",
                    message = "Request body must be valid JSON."
                )
            )
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

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
                    JSONObject()
                        .put("performed", true)
                        .put("count", result.performedCount)
                        .put("direction", direction)
                        .put("attemptedPath", result.attemptedPath)
                        .put("contentChanged", observation.surfaceChanged)
                        .put("surfaceChanged", observation.surfaceChanged)
                        .put("beforeSnapshotToken", observation.beforeSnapshotToken)
                        .put("afterSnapshotToken", observation.afterSnapshotToken)
                        .put("finalPackageName", observation.finalPackageName)
                        .put("finalActivity", observation.finalActivity)
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

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
                JSONObject()
                    .put("changed", result.changed)
                    .put("elapsedMs", result.elapsedMs)
                    .put("snapshotToken", result.snapshotToken ?: JSONObject.NULL)
                    .put("packageName", result.packageName ?: JSONObject.NULL)
                    .put("activity", result.activity ?: JSONObject.NULL)
            )
        )
    }

    private fun buildWaitResponse(requestBody: String): String {
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }

        val condition = body.optJSONObject("condition")
            ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "condition is required."))

        val strategy = condition.optString("strategy").trim()
        if (strategy.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "condition.strategy is required."))
        }

        if (strategy !in SUPPORTED_WAIT_STRATEGIES) {
            return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Unsupported /wait strategy: $strategy."))
        }

        val query = condition.opt("query").let { value ->
            when {
                value == null || value == JSONObject.NULL -> null
                else -> value.toString()
            }
        }

        val timeoutMs = body.optLongOrNull("timeoutMs") ?: 5000L
        if (timeoutMs < 0) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "timeoutMs must be non-negative."))
        }

        val intervalMs = body.optLongOrNull("intervalMs") ?: 200L
        if (intervalMs < 50) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "intervalMs must be at least 50."))
        }

        val result = stateCoordinator.waitForCondition(strategy, query, timeoutMs, intervalMs)

        return if (result.satisfied) {
            buildJsonResponse(200, successEnvelope(JSONObject()
                .put("satisfied", true)
                .put("elapsedMs", result.elapsedMs)
                .put("node", result.node?.let { node ->
                    JSONObject()
                        .put("nodeId", node.nodeId)
                        .put("text", node.text ?: JSONObject.NULL)
                        .put("contentDesc", node.contentDesc ?: JSONObject.NULL)
                        .put("resourceId", node.resourceId ?: JSONObject.NULL)
                } ?: JSONObject.NULL)))
        } else {
            buildJsonResponse(200, successEnvelope(JSONObject()
                .put("satisfied", false)
                .put("elapsedMs", result.elapsedMs)
                .put("reason", result.attemptedPath)))
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }
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
        val body = try {
            JSONObject(requestBody.ifBlank { "{}" })
        } catch (_: JSONException) {
            return buildJsonResponse(400, errorEnvelope("BAD_REQUEST", "Request body must be valid JSON."))
        }
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

    private fun successEnvelope(data: JSONObject): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put("data", data)
            .put("meta", buildMeta())
    }

    private fun errorEnvelope(code: String, message: String): JSONObject {
        return JSONObject()
            .put("ok", false)
            .put("error", JSONObject()
                .put("code", code)
                .put("message", message)
                .put("details", JSONObject())
            )
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

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            serverSocket = null
        }
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 5583
        const val LOG_TAG = "LocalApiServer"
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

private fun jsonOptIntOrNull(json: JSONObject, key: String): Int? {
    val value = json.opt(key)
    return if (value is Number) value.toInt() else null
}
