/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import android.util.Log
import com.folklore25.ghosthand.interaction.effects.ActionEvidencePayloads
import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.optLongOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.ScrollFailureReason
import com.folklore25.ghosthand.SwipeFailureReason
import com.folklore25.ghosthand.state.ScrollBatchResult
import com.folklore25.ghosthand.state.StateCoordinator
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import org.json.JSONObject

internal class ActionMotionRouteHandlers(
    private val stateCoordinator: StateCoordinator,
    private val observationPublisher: GhosthandObservationPublisher
) {
    fun buildSwipeResponse(requestBody: String): String {
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
            swipeResult.performed -> {
                val actionEffect = observation.toActionEffectObservation()
                val postActionState = PostActionStateComposer.fromObservedEffect(
                    actionEffect = actionEffect,
                    fallbackSnapshot = observation.afterSnapshot
                )
                observationPublisher.recordActionCompleted(
                    route = "/swipe",
                    attemptedPath = swipeResult.attemptedPath,
                    backendUsed = swipeResult.backendUsed,
                    actionEffect = actionEffect,
                    postActionState = postActionState
                )
                buildJsonResponse(
                    200,
                    successEnvelope(
                        data = JSONObject(
                            ActionEvidencePayloads.commonFields(
                                performed = true,
                                backendUsed = swipeResult.backendUsed,
                                attemptedPath = swipeResult.attemptedPath,
                                requestShape = swipeCoordinates.requestShape,
                                actionEffect = actionEffect,
                                postActionState = postActionState,
                                extras = linkedMapOf(
                                    "contentChanged" to observation.surfaceChanged
                                )
                            )
                        ),
                        disclosure = buildMotionDisclosure("/swipe", swipeResult.performed, observation.surfaceChanged)
                    )
                )
            }

            swipeResult.failureReason == SwipeFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for swipe execution."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Accessibility swipe action failed."))
        }
    }

    fun buildScrollResponse(requestBody: String): String {
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
            result.performed -> {
                val actionEffect = observation.toActionEffectObservation()
                val postActionState = PostActionStateComposer.fromObservedEffect(
                    actionEffect = actionEffect,
                    fallbackSnapshot = observation.afterSnapshot
                )
                observationPublisher.recordActionCompleted(
                    route = "/scroll",
                    attemptedPath = result.attemptedPath,
                    actionEffect = actionEffect,
                    postActionState = postActionState
                )
                buildJsonResponse(
                    200,
                    successEnvelope(
                        data = JSONObject(
                            ActionEvidencePayloads.commonFields(
                                performed = true,
                                attemptedPath = result.attemptedPath,
                                actionEffect = actionEffect,
                                postActionState = postActionState,
                                extras = linkedMapOf(
                                    "count" to result.performedCount,
                                    "direction" to direction,
                                    "contentChanged" to observation.surfaceChanged,
                                    "surfaceChanged" to observation.surfaceChanged
                                )
                            )
                        ),
                        disclosure = buildMotionDisclosure("/scroll", result.performed, observation.surfaceChanged)
                    )
                )
            }

            result.failureReason == ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available."))
            result.failureReason == ScrollFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Scroll target node was not found."))
            result.failureReason == ScrollFailureReason.INVALID_DIRECTION ->
                buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "Invalid scroll direction."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Scroll gesture failed."))
        }
    }

    private companion object {
        const val SWIPE_LOG_TAG = "GhostSwipe"
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

internal fun buildMotionDisclosure(route: String, performed: Boolean, surfaceChanged: Boolean): GhosthandDisclosure? {
    if (!performed || surfaceChanged) return null
    return GhosthandDisclosure(
        kind = "ambiguity",
        summary = "$route dispatched successfully, but Ghosthand did not observe visible-state change yet.",
        assumptionToCorrect = "`performed=true` proves the content advanced.",
        nextBestActions = listOf("Use GET /wait to allow the surface to settle.", "Use /screen to confirm whether visible content actually changed.")
    )
}
