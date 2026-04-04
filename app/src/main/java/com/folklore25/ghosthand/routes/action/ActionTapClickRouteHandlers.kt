/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import android.util.Log
import com.folklore25.ghosthand.ClickAttemptResult
import com.folklore25.ghosthand.ClickFailureReason
import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.payload.GhosthandDisclosurePayloads
import com.folklore25.ghosthand.payload.GhosthandInteractionPayloads
import com.folklore25.ghosthand.payload.GhosthandPayloadJsonSupport
import com.folklore25.ghosthand.interaction.effects.ActionEvidencePayloads
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.parseSelector
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import com.folklore25.ghosthand.TapFailureReason
import org.json.JSONObject

internal class ActionTapClickRouteHandlers(
    private val stateCoordinator: StateCoordinator,
    private val observationPublisher: GhosthandObservationPublisher
) {
    fun buildTapResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/tap")
            ?: return badJsonBodyResponse()
        val backend = body.optString("backend", DEFAULT_BACKEND).ifBlank { DEFAULT_BACKEND }
        when (backend) {
            BACKEND_AUTO, BACKEND_ACCESSIBILITY -> Unit
            else -> return unsupportedBackendResponse()
        }

        val directX = body.optIntOrNull("x")
        val directY = body.optIntOrNull("y")
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
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
        val observation = if (tapResult.performed) {
            observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }
        val actionEffect = if (tapResult.performed) observation.toActionEffectObservation() else null
        val postActionState = PostActionStateComposer.fromObservedEffect(
            actionEffect = actionEffect,
            fallbackSnapshot = observation.afterSnapshot
        )

        Log.i(TAP_LOG_TAG, "event=tap_request backendRequested=$backend backendUsed=${tapResult.backendUsed ?: "none"} tapPath=${tapResult.attemptedPath} success=${tapResult.performed} failure=${tapResult.failureReason ?: "none"}")
        return when {
            tapResult.performed -> {
                observationPublisher.recordActionCompleted(
                    route = "/tap",
                    attemptedPath = tapResult.attemptedPath,
                    backendUsed = tapResult.backendUsed,
                    actionEffect = actionEffect,
                    postActionState = postActionState
                )
                buildJsonResponse(
                    200,
                    successEnvelope(
                        JSONObject(
                            ActionEvidencePayloads.commonFields(
                                performed = true,
                                backendUsed = tapResult.backendUsed,
                                attemptedPath = tapResult.attemptedPath,
                                actionEffect = actionEffect,
                                postActionState = postActionState
                            )
                        ),
                        disclosure = buildActionEffectDisclosure("/tap", true, actionEffect?.stateChanged ?: false)
                    )
                )
            }

            tapResult.failureReason == TapFailureReason.ACCESSIBILITY_UNAVAILABLE ->
                buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Accessibility service is not available for tap execution."))
            tapResult.failureReason == TapFailureReason.NODE_NOT_FOUND ->
                buildJsonResponse(422, errorEnvelope("NODE_NOT_FOUND", "Tap target node was not found."))
            else -> buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Accessibility tap action failed."))
        }
    }

    fun buildClickResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/click")
            ?: return badJsonBodyResponse()
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val nodeId = body.optString("nodeId").trim()
        val nodeIdProvided = nodeId.isNotEmpty()
        val selector = if (!nodeIdProvided) parseSelector(body) else null
        val initialClickResult = if (nodeIdProvided) {
            stateCoordinator.clickNode(nodeId)
        } else {
            val requiredSelector = selector
                ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "nodeId or one of text, desc, id, or strategy is required."))
            stateCoordinator.clickFirstMatchFresh(
                strategy = requiredSelector.strategy,
                query = requiredSelector.query ?: "",
                clickableOnly = clickSelectorRequiresClickableTarget(body),
                index = body.optIntOrNull("index") ?: 0
            )
        }
        val observation = if (initialClickResult.performed) {
            observeActionSurfaceChange(beforeSnapshot) {
                stateCoordinator.getTreeSnapshotResult().snapshot
            }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }
        val clickResult = if (initialClickResult.performed) {
            initialClickResult.copy(effect = observation.toActionEffectObservation())
        } else {
            initialClickResult
        }
        val postActionState = PostActionStateComposer.fromObservedEffect(
            actionEffect = clickResult.effect,
            fallbackSnapshot = observation.afterSnapshot
        )

        Log.i(CLICK_LOG_TAG, "event=click_request nodeId=$nodeId clickPath=${clickResult.attemptedPath} success=${clickResult.performed} failure=${clickResult.failureReason?.name ?: "none"}")
        val selectorStrategy = selector?.strategy
        val clickableOnly = if (!nodeIdProvided) clickSelectorRequiresClickableTarget(body) else false

        return when {
            clickResult.performed -> {
                observationPublisher.recordActionCompleted(
                    route = "/click",
                    attemptedPath = clickResult.attemptedPath,
                    backendUsed = clickResult.backendUsed,
                    actionEffect = clickResult.effect,
                    postActionState = postActionState
                )
                buildJsonResponse(
                    200,
                    successEnvelope(
                        data = GhosthandPayloadJsonSupport.fieldsToJson(
                            GhosthandInteractionPayloads.clickFields(
                                clickResult,
                                fallbackSnapshot = observation.afterSnapshot
                            )
                        ),
                        disclosure = buildClickDisclosure(selectorStrategy, clickableOnly, clickResult)
                            ?: buildActionEffectDisclosure("/click", clickResult.performed, clickResult.effect?.stateChanged ?: false)
                    )
                )
            }

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

    private companion object {
        const val TAP_LOG_TAG = "GhostTap"
        const val CLICK_LOG_TAG = "GhostClick"
    }
}

internal fun clickSelectorRequiresClickableTarget(body: JSONObject): Boolean {
    return if (body.has("clickable")) body.optBoolean("clickable", false) else true
}

internal fun buildClickFailureDetails(result: ClickAttemptResult, clickableOnly: Boolean): JSONObject {
    val missHint = result.selectorMissHint ?: return JSONObject()
    return JSONObject(GhosthandDisclosurePayloads.clickFailureFields(missHint)).apply {
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
