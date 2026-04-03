/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.input

import android.util.Log
import com.folklore25.ghosthand.GhosthandApiPayloads
import com.folklore25.ghosthand.InputKeyFailureReason
import com.folklore25.ghosthand.InputOperationResult
import com.folklore25.ghosthand.SetTextFailureReason
import com.folklore25.ghosthand.StateCoordinator
import com.folklore25.ghosthand.TypeFailureReason
import com.folklore25.ghosthand.routes.action.putPostActionState
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.server.LocalApiServerRoute
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import org.json.JSONObject

internal class InputRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("POST", "/type") { request -> buildTypeResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/input") { request -> buildInputResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/setText") { request -> buildSetTextResponse(request.requestBody) }
        )
    }

    private fun buildTypeResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/type")
            ?: return badJsonBodyResponse()
        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Only backend=accessibility and backend=auto are supported."))
        }
        if (!body.has("text") || body.isNull("text")) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }
        val rawText = body.opt("text")
        if (rawText !is String) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text must be a string."))
        }
        val typeResult = stateCoordinator.typeText(rawText)
        Log.i(TYPE_LOG_TAG, "event=type_request backendRequested=$backend backendUsed=${typeResult.backendUsed ?: "none"} typePath=${typeResult.attemptedPath} success=${typeResult.performed} failure=${typeResult.failureReason ?: "none"} textLength=${rawText.length}")
        return when {
            typeResult.performed ->
                buildJsonResponse(200, successEnvelope(JSONObject().put("performed", true).put("backendUsed", typeResult.backendUsed)))
            typeResult.failureReason == TypeFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for text input."))
            else ->
                buildJsonResponse(
                    422,
                    errorEnvelope(
                        "ACCESSIBILITY_ACTION_FAILED",
                        if (typeResult.failureReason == TypeFailureReason.NO_EDITABLE_TARGET) {
                            "No focused editable target is available for text input."
                        } else {
                            "Accessibility text input action failed."
                        }
                    )
                )
        }
    }

    private fun buildInputResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/input")
            ?: return badJsonBodyResponse()
        val parsedRequest = GhosthandApiPayloads.parseInputRequest(body)
        if (parsedRequest.errorMessage != null || parsedRequest.request == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", parsedRequest.errorMessage ?: "Invalid /input request."))
        }
        val typeResult = stateCoordinator.performInput(parsedRequest.request)
        val payloadResult = typeResult.copy(
            postActionState = PostActionStateComposer.fromObservedEffect(
                actionEffect = null,
                fallbackSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
            )
        )
        val resultPayload = GhosthandApiPayloads.inputResultJson(payloadResult)
        Log.i(INPUT_LOG_TAG, "event=input_request textAction=${parsedRequest.request.textAction?.wireValue ?: "none"} key=${parsedRequest.request.key?.wireValue ?: "none"} success=${typeResult.performed}")
        return when {
            typeResult.performed -> buildJsonResponse(200, successEnvelope(resultPayload))
            hasInputAccessibilityUnavailable(typeResult) ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for one or more requested /input operations.", resultPayload))
            else ->
                buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", buildInputFailureMessage(typeResult), resultPayload))
        }
    }

    private fun buildSetTextResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/setText")
            ?: return badJsonBodyResponse()
        val nodeId = body.optString("nodeId").trim()
        if (nodeId.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "nodeId is required."))
        }
        if (!body.has("text")) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }
        val rawText = body.opt("text")
        val text = if (rawText is String) rawText else ""
        val setTextResult = stateCoordinator.setTextOnNode(nodeId, text)
        Log.i(SETTEXT_LOG_TAG, "event=settext_request nodeId=$nodeId settextPath=${setTextResult.attemptedPath} success=${setTextResult.performed}")
        return when {
            setTextResult.performed ->
                buildJsonResponse(
                    200,
                    successEnvelope(
                        JSONObject()
                            .put("performed", true)
                            .put("backendUsed", setTextResult.backendUsed)
                            .putPostActionState(
                                PostActionStateComposer.fromObservedEffect(
                                    actionEffect = null,
                                    fallbackSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
                                )
                            )
                    )
                )
            setTextResult.failureReason == SetTextFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for text setting."))
            setTextResult.failureReason == SetTextFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Target node was not found."))
            setTextResult.failureReason == SetTextFailureReason.NODE_NOT_EDITABLE ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_EDITABLE", "Target node is not editable or not enabled."))
            else ->
                buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Set text action failed on the target node."))
        }
    }

    private companion object {
        const val BACKEND_AUTO = "auto"
        const val BACKEND_ACCESSIBILITY = "accessibility"
        const val DEFAULT_BACKEND = "auto"
        const val TYPE_LOG_TAG = "GhostType"
        const val INPUT_LOG_TAG = "GhostInput"
        const val SETTEXT_LOG_TAG = "GhostSetText"
    }
}

private fun hasInputAccessibilityUnavailable(result: InputOperationResult): Boolean {
    return result.textMutation?.failureReason == TypeFailureReason.ACCESSIBILITY_UNAVAILABLE ||
        result.keyDispatch?.failureReason == InputKeyFailureReason.ACCESSIBILITY_UNAVAILABLE
}

private fun buildInputFailureMessage(result: InputOperationResult): String {
    if (result.textMutation != null && result.keyDispatch != null) {
        return "One or more explicit /input operations failed."
    }
    return when {
        result.textMutation?.failureReason == TypeFailureReason.NO_EDITABLE_TARGET ->
            "No focused editable target is available for text input."
        result.textMutation != null ->
            "Accessibility text input action failed."
        result.keyDispatch?.failureReason == InputKeyFailureReason.NO_EDITABLE_TARGET ->
            "No focused editable target is available for key dispatch."
        else ->
            "Accessibility key dispatch failed."
    }
}
