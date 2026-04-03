/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.ClickAttemptResult
import com.folklore25.ghosthand.ClickFailureReason
import com.folklore25.ghosthand.GesturePoint
import com.folklore25.ghosthand.GestureStroke
import com.folklore25.ghosthand.GhosthandApiPayloads
import com.folklore25.ghosthand.GhosthandDisclosure
import com.folklore25.ghosthand.PostActionState
import com.folklore25.ghosthand.ScrollBatchResult
import com.folklore25.ghosthand.ScrollFailureReason
import com.folklore25.ghosthand.SelectorQuery
import com.folklore25.ghosthand.SetTextFailureReason
import com.folklore25.ghosthand.StateCoordinator
import com.folklore25.ghosthand.SwipeFailureReason
import com.folklore25.ghosthand.TapFailureReason
import com.folklore25.ghosthand.TypeFailureReason
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.optLongOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.parseSelector
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.server.LocalApiServerRoute
import org.json.JSONObject

internal class ActionRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("POST", "/tap") { request -> buildTapResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/swipe") { request -> buildSwipeResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/click") { request -> buildClickResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/scroll") { request -> buildScrollResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/longpress") { request -> buildLongpressResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/gesture") { request -> buildGestureResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/back") { buildGlobalActionResponse("back", AccessibilityService.GLOBAL_ACTION_BACK) },
            LocalApiServerRoute("POST", "/home") { buildGlobalActionResponse("home", AccessibilityService.GLOBAL_ACTION_HOME) },
            LocalApiServerRoute("POST", "/recents") { buildGlobalActionResponse("recents", AccessibilityService.GLOBAL_ACTION_RECENTS) }
        )
    }

    private fun buildTapResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/tap")
            ?: return badJsonBodyResponse()
        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> return unsupportedBackendResponse()
        }

        val directX = body.optIntOrNull("x")
        val directY = body.optIntOrNull("y")
        val tapResult = if (directX != null && directY != null) {
            stateCoordinator.tapPoint(directX, directY)
        } else {
            val target = body.optJSONObject("target") ?: return buildJsonResponse(
                400,
                errorEnvelope("INVALID_ARGUMENT", "Either x/y or target is required.")
            )
            when (target.optString("type").trim()) {
                TARGET_TYPE_POINT -> {
                    val x = target.optIntOrNull("x")
                    val y = target.optIntOrNull("y")
                    if (x == null || y == null) {
                        return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "point target requires integer x and y."))
                    }
                    stateCoordinator.tapPoint(x, y)
                }

                TARGET_TYPE_NODE -> {
                    val nodeId = target.optString("nodeId").trim()
                    if (nodeId.isEmpty()) {
                        return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "node target requires nodeId."))
                    }
                    stateCoordinator.tapNode(nodeId)
                }

                "" -> return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "target.type is required."))
                else -> return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Unsupported tap target type: ${target.optString("type").trim()}."))
            }
        }

        Log.i(TAP_LOG_TAG, "event=tap_request backendRequested=$backend backendUsed=${tapResult.backendUsed ?: "none"} tapPath=${tapResult.attemptedPath} success=${tapResult.performed} failure=${tapResult.failureReason ?: "none"}")
        return when {
            tapResult.performed -> buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject()
                        .put("performed", true)
                        .put("backendUsed", tapResult.backendUsed)
                        .putPostActionState(
                            PostActionStateComposer.fromObservedEffect(
                                actionEffect = null,
                                fallbackSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
                            )
                        )
                )
            )

            tapResult.failureReason == TapFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for tap execution."))
            tapResult.failureReason == TapFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Tap target node was not found."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Accessibility tap action failed."))
        }
    }

    private fun buildSwipeResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/swipe")
            ?: return badJsonBodyResponse()
        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> return unsupportedBackendResponse()
        }

        val swipeCoordinates = parseSwipeCoordinates(body)
        if (!swipeCoordinates.valid) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", swipeCoordinates.errorMessage ?: "Swipe coordinates are invalid."))
        }
        val durationMs = body.optLongOrNull("durationMs")
        if (durationMs == null || durationMs <= 0L || durationMs > MAX_SWIPE_DURATION_MS) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "durationMs must be between 1 and $MAX_SWIPE_DURATION_MS."))
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
            observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }

        Log.i(SWIPE_LOG_TAG, "event=swipe_request backendRequested=$backend backendUsed=${swipeResult.backendUsed ?: "none"} swipePath=${swipeResult.attemptedPath} success=${swipeResult.performed} failure=${swipeResult.failureReason ?: "none"} from=${swipeCoordinates.fromX},${swipeCoordinates.fromY} to=${swipeCoordinates.toX},${swipeCoordinates.toY} durationMs=$durationMs requestShape=${swipeCoordinates.requestShape}")
        return when {
            swipeResult.performed -> buildJsonResponse(
                200,
                successEnvelope(
                    data = JSONObject()
                        .put("performed", true)
                        .put("backendUsed", swipeResult.backendUsed)
                        .put("requestShape", swipeCoordinates.requestShape)
                        .put("contentChanged", observation.surfaceChanged)
                        .put("beforeSnapshotToken", observation.beforeSnapshotToken)
                        .put("afterSnapshotToken", observation.afterSnapshotToken)
                        .put("finalPackageName", observation.finalPackageName)
                        .put("finalActivity", observation.finalActivity)
                        .putPostActionState(
                            PostActionStateComposer.fromObservedEffect(
                                actionEffect = observation.toActionEffectObservation(),
                                fallbackSnapshot = null
                            )
                        ),
                    disclosure = buildMotionDisclosure("/swipe", swipeResult.performed, observation.surfaceChanged)
                )
            )

            swipeResult.failureReason == SwipeFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for swipe execution."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Accessibility swipe action failed."))
        }
    }

    private fun buildClickResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/click")
            ?: return badJsonBodyResponse()
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val nodeId = body.optString("nodeId").trim()
        val nodeIdProvided = nodeId.isNotEmpty()
        val initialClickResult = if (nodeIdProvided) {
            stateCoordinator.clickNode(nodeId)
        } else {
            val selector = parseSelector(body)
                ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "nodeId or one of text, desc, id, or strategy is required."))
            stateCoordinator.clickFirstMatchFresh(
                strategy = selector.strategy,
                query = selector.query ?: "",
                clickableOnly = clickSelectorRequiresClickableTarget(body),
                index = body.optIntOrNull("index") ?: 0
            )
        }
        val clickResult = if (initialClickResult.performed) {
            initialClickResult.copy(effect = observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }.toActionEffectObservation())
        } else {
            initialClickResult
        }

        Log.i(CLICK_LOG_TAG, "event=click_request nodeId=$nodeId clickPath=${clickResult.attemptedPath} success=${clickResult.performed} failure=${clickResult.failureReason?.name ?: "none"}")
        val selectorStrategy = if (!nodeIdProvided) parseSelector(body)?.strategy else null
        val clickableOnly = if (!nodeIdProvided) clickSelectorRequiresClickableTarget(body) else false

        return when {
            clickResult.performed -> buildJsonResponse(
                200,
                successEnvelope(
                    data = GhosthandApiPayloads.clickPayload(clickResult),
                    disclosure = buildClickDisclosure(selectorStrategy, clickableOnly, clickResult)
                        ?: buildActionEffectDisclosure("/click", clickResult.performed, clickResult.effect?.stateChanged ?: false)
                )
            )

            clickResult.failureReason == ClickFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for click execution."))
            clickResult.failureReason == ClickFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(
                    422,
                    errorEnvelope(
                        code = clickFailureErrorCode(clickResult, nodeIdProvided),
                        message = clickFailureMessage(clickResult, nodeIdProvided),
                        details = buildClickFailureDetails(clickResult, clickableOnly),
                        disclosure = buildStaleNodeReferenceDisclosure(clickResult, nodeIdProvided)
                            ?: buildClickDisclosure(selectorStrategy, clickableOnly, clickResult)
                    )
                )
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Accessibility click action failed."))
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
            stateCoordinator.scroll(direction = direction, target = target, count = count)
        }
        val observation = if (result.performed) {
            observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }
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
                        .put("finalActivity", observation.finalActivity)
                        .putPostActionState(
                            PostActionStateComposer.fromObservedEffect(
                                actionEffect = observation.toActionEffectObservation(),
                                fallbackSnapshot = null
                            )
                        ),
                    disclosure = buildMotionDisclosure("/scroll", result.performed, observation.surfaceChanged)
                )
            )

            result.failureReason == ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available."))
            result.failureReason == ScrollFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Scroll target node was not found."))
            result.failureReason == ScrollFailureReason.INVALID_DIRECTION ->
                buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "Invalid scroll direction."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Scroll gesture failed."))
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
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject().put("performed", true).putPostActionState(
                        PostActionStateComposer.fromObservedEffect(
                            actionEffect = null,
                            fallbackSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
                        )
                    )
                )
            )
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
            return gestureResponse(stateCoordinator.performGesture(strokes))
        }

        val strokesJson = body.optJSONArray("strokes")
            ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "strokes is required."))
        val strokes = mutableListOf<GestureStroke>()
        for (i in 0 until strokesJson.length()) {
            val strokeJson = strokesJson.optJSONObject(i) ?: continue
            val pointsJson = strokeJson.optJSONArray("points") ?: continue
            val points = mutableListOf<GesturePoint>()
            for (j in 0 until pointsJson.length()) {
                val pt = pointsJson.optJSONObject(j) ?: continue
                val px = pt.optIntOrNull("x") ?: continue
                val py = pt.optIntOrNull("y") ?: continue
                points.add(GesturePoint(px, py))
            }
            if (points.isNotEmpty()) {
                strokes.add(GestureStroke(points, strokeJson.optLongOrNull("durationMs") ?: 300L))
            }
        }
        if (strokes.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "At least one valid stroke with points is required."))
        }
        return gestureResponse(stateCoordinator.performGesture(strokes))
    }

    private fun gestureResponse(performed: Boolean): String {
        return if (performed) {
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject().put("performed", true).putPostActionState(
                        PostActionStateComposer.fromObservedEffect(
                            actionEffect = null,
                            fallbackSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
                        )
                    )
                )
            )
        } else {
            buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Gesture dispatch failed."))
        }
    }

    private fun buildGlobalActionResponse(actionName: String, actionCode: Int): String {
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val initialResult = stateCoordinator.performGlobalAction(actionCode)
        val result = if (initialResult.performed) {
            initialResult.copy(effect = observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }.toActionEffectObservation())
        } else {
            initialResult
        }
        return if (result.performed) {
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject(GhosthandApiPayloads.globalActionFields(result)),
                    disclosure = buildActionEffectDisclosure("/$actionName", result.performed, result.effect?.stateChanged ?: false)
                )
            )
        } else {
            buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Global action '$actionName' failed. Accessibility service may not be connected."))
        }
    }

    private fun unsupportedBackendResponse(): String {
        return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Only backend=accessibility and backend=auto are supported."))
    }

    private companion object {
        const val TARGET_TYPE_POINT = "point"
        const val TARGET_TYPE_NODE = "node"
        const val BACKEND_AUTO = "auto"
        const val BACKEND_ACCESSIBILITY = "accessibility"
        const val DEFAULT_BACKEND = "auto"
        const val TAP_LOG_TAG = "GhostTap"
        const val SWIPE_LOG_TAG = "GhostSwipe"
        const val CLICK_LOG_TAG = "GhostClick"
        const val MAX_SWIPE_DURATION_MS = 5000L
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
        val fromX = from.optIntOrNull("x")
        val fromY = from.optIntOrNull("y")
        val toX = to.optIntOrNull("x")
        val toY = to.optIntOrNull("y")
        return if (fromX == null || fromY == null || toX == null || toY == null) {
            ParsedSwipeCoordinates(valid = false, errorMessage = "from/to must contain integer x and y values.")
        } else {
            ParsedSwipeCoordinates(true, fromX, fromY, toX, toY, "from_to")
        }
    }
    val x1 = body.optIntOrNull("x1")
    val y1 = body.optIntOrNull("y1")
    val x2 = body.optIntOrNull("x2")
    val y2 = body.optIntOrNull("y2")
    return if (x1 != null && y1 != null && x2 != null && y2 != null) {
        ParsedSwipeCoordinates(true, x1, y1, x2, y2, "xy_alias")
    } else {
        ParsedSwipeCoordinates(valid = false, errorMessage = "Use canonical from/to point objects or the x1/y1/x2/y2 alias form.")
    }
}

internal fun clickSelectorRequiresClickableTarget(body: JSONObject): Boolean {
    return if (body.has("clickable")) body.optBoolean("clickable", false) else true
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
    val surfaceChanged = (beforeToken != null && afterToken != null && beforeToken != afterToken) ||
        beforePackage != afterPackage ||
        beforeActivity != afterActivity
    return ScrollSurfaceObservation(surfaceChanged, beforeToken, afterToken, afterPackage, afterActivity)
}

internal fun observeActionSurfaceChange(
    beforeSnapshot: AccessibilityTreeSnapshot?,
    snapshotProvider: () -> AccessibilityTreeSnapshot?
): ScrollSurfaceObservation {
    Thread.sleep(300L)
    return observeScrollSurfaceChange(beforeSnapshot, snapshotProvider())
}

internal fun ScrollSurfaceObservation.toActionEffectObservation(): ActionEffectObservation {
    return ActionEffectObservation(surfaceChanged, beforeSnapshotToken, afterSnapshotToken, finalPackageName, finalActivity)
}

internal fun JSONObject.putPostActionState(state: PostActionState?): JSONObject {
    state
        ?.let(GhosthandApiPayloads::postActionStateFields)
        ?.takeIf { it.isNotEmpty() }
        ?.let { put("postActionState", JSONObject(it)) }
    return this
}

internal fun buildMotionDisclosure(route: String, performed: Boolean, surfaceChanged: Boolean): GhosthandDisclosure? {
    if (!performed || surfaceChanged) return null
    return GhosthandDisclosure(
        kind = "ambiguity",
        summary = "$route dispatched successfully, but Ghosthand did not observe visible-state change yet.",
        assumptionToCorrect = "`performed=true` proves the content advanced.",
        nextBestActions = listOf("Use GET /wait to allow the surface to settle.", "Use /screen to confirm whether visible content actually changed.")
    )
}

internal fun buildActionEffectDisclosure(route: String, performed: Boolean, stateChanged: Boolean): GhosthandDisclosure? {
    if (!performed || stateChanged) return null
    return GhosthandDisclosure(
        kind = "ambiguity",
        summary = "$route dispatched successfully, but Ghosthand did not observe visible-state change yet.",
        assumptionToCorrect = "`performed=true` proves the UI changed.",
        nextBestActions = listOf("Use GET /wait to allow the surface to settle.", "Use /screen to confirm whether visible content actually changed.")
    )
}

internal fun buildClickFailureDetails(result: ClickAttemptResult, clickableOnly: Boolean): JSONObject {
    val missHint = result.selectorMissHint ?: return JSONObject()
    return JSONObject(GhosthandApiPayloads.clickFailureFields(missHint)).apply {
        put("failureCategory", missHint.failureCategory ?: "no_selector_match")
        put("clickableOnly", clickableOnly)
    }
}

internal fun clickFailureErrorCode(result: ClickAttemptResult, nodeIdProvided: Boolean): String {
    return if (isStaleNodeReferenceFailure(result, nodeIdProvided)) "STALE_NODE_REFERENCE" else "NODE_NOT_FOUND"
}

internal fun clickFailureMessage(result: ClickAttemptResult, nodeIdProvided: Boolean): String {
    return if (isStaleNodeReferenceFailure(result, nodeIdProvided)) {
        "Click target node reference expired because the UI snapshot changed."
    } else {
        "Click target node was not found."
    }
}

internal fun buildStaleNodeReferenceDisclosure(result: ClickAttemptResult, nodeIdProvided: Boolean): GhosthandDisclosure? {
    if (!isStaleNodeReferenceFailure(result, nodeIdProvided)) return null
    return GhosthandDisclosure(
        kind = "constraint",
        summary = "nodeId references are only valid for the snapshot they came from; this saved reference expired after the UI changed.",
        assumptionToCorrect = "A saved nodeId stays valid across later UI snapshots.",
        nextBestActions = listOf(
            "Refresh the surface with /tree or /screen, then retry /click with a fresh nodeId.",
            "Use selector-based /click or /find if the surface may have changed."
        )
    )
}

internal fun isStaleNodeReferenceFailure(result: ClickAttemptResult, nodeIdProvided: Boolean): Boolean {
    return nodeIdProvided && result.failureReason == ClickFailureReason.NODE_NOT_FOUND && result.attemptedPath == "stale_snapshot"
}

internal fun buildClickDisclosure(strategy: String?, clickableOnly: Boolean, result: ClickAttemptResult): GhosthandDisclosure? {
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
        val missHint = result.selectorMissHint
        if (clickableOnly && missHint?.failureCategory == "actionable_target_not_found") {
            return GhosthandDisclosure(
                kind = "discoverability",
                summary = "Selector-based click found label matches on the requested surface, but none resolved to an actionable target.",
                assumptionToCorrect = "A visible label match is always directly actionable.",
                nextBestActions = listOf(
                    "Use /find without clickable=true to inspect the matched node before escalating.",
                    alternateSelectorAction(strategy)
                )
            )
        }
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
                if (clickableOnly) "Use /find without clickable=true to inspect the matched node first." else "Use /find to inspect whether the label is present on a different surface.",
                alternateSelectorAction(strategy)
            )
        )
    }
    return null
}

private fun alternateSelectorAction(strategy: String): String {
    return when (strategy) {
        "text", "textContains" -> "Retry with desc if the meaningful label lives in content descriptions."
        "contentDesc", "contentDescContains" -> "Retry with text if the visible label is rendered as text."
        "resourceId" -> "Retry with text or desc if you only know the visible label."
        else -> "Retry with text, desc, or id based on the surface you can actually observe."
    }
}
