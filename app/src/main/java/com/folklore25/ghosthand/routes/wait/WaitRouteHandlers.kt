/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.wait

import com.folklore25.ghosthand.FlatAccessibilityNode
import com.folklore25.ghosthand.GhosthandDisclosure
import com.folklore25.ghosthand.StateCoordinator
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optLongOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.parseSelector
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.server.LocalApiServerRoute
import org.json.JSONObject

internal class WaitRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("GET", "/wait") { request -> buildWaitUiChangeResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/wait") { request -> buildWaitResponse(request.requestBody) }
        )
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

        val timeoutMs = body.optLongOrNull("timeoutMs") ?: 5000L
        if (timeoutMs < 0) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "timeoutMs must be non-negative."))
        }

        val intervalMs = body.optLongOrNull("intervalMs") ?: 200L
        if (intervalMs < 50) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "intervalMs must be at least 50."))
        }

        val result = stateCoordinator.waitForCondition(strategy, selector.query, timeoutMs, intervalMs)
        val normalized = normalizeWaitConditionResult(result)

        return if (normalized.satisfied) {
            buildJsonResponse(
                200,
                successEnvelope(
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
                )
            )
        } else {
            buildJsonResponse(
                200,
                successEnvelope(
                    data = JSONObject()
                        .put("satisfied", false)
                        .put("conditionMet", normalized.conditionMet)
                        .put("stateChanged", normalized.stateChanged)
                        .put("timedOut", normalized.timedOut)
                        .put("elapsedMs", result.elapsedMs)
                        .put("reason", normalized.reason),
                    disclosure = buildWaitConditionDisclosure(strategy, normalized)
                )
            )
        }
    }

    private companion object {
        val SUPPORTED_WAIT_STRATEGIES = setOf(
            "text",
            "textContains",
            "resourceId",
            "contentDesc",
            "contentDescContains",
            "focused"
        )
    }
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

private fun selectorAliasForStrategy(strategy: String): String {
    return when (strategy) {
        "contentDesc", "contentDescContains" -> "desc"
        "resourceId" -> "id"
        "focused" -> "focused"
        else -> "text"
    }
}
