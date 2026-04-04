/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import com.folklore25.ghosthand.state.summary.PostActionStateComposer

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.payload.GhosthandInteractionPayloads
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONObject

internal class ActionNavigationRouteHandlers(
    private val stateCoordinator: StateCoordinator,
    private val observationPublisher: GhosthandObservationPublisher
) {
    fun buildGlobalActionResponse(actionName: String, actionCode: Int): String {
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val initialResult = stateCoordinator.performGlobalAction(actionCode)
        val observation = if (initialResult.performed) {
            observeActionSurfaceChange(beforeSnapshot) {
                stateCoordinator.getTreeSnapshotResult().snapshot
            }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }
        val result = if (initialResult.performed) {
            initialResult.copy(
                effect = observation.toActionEffectObservation()
            )
        } else {
            initialResult
        }
        return if (result.performed) {
            val postActionState = com.folklore25.ghosthand.state.summary.PostActionStateComposer.fromObservedEffect(
                actionEffect = result.effect,
                fallbackSnapshot = observation.afterSnapshot
            )
            observationPublisher.recordActionCompleted(
                route = "/$actionName",
                attemptedPath = result.attemptedPath,
                actionEffect = result.effect,
                postActionState = postActionState
            )
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject(GhosthandInteractionPayloads.globalActionFields(result, observation.afterSnapshot)),
                    disclosure = buildActionEffectDisclosure("/$actionName", result.performed, result.effect?.stateChanged ?: false)
                )
            )
        } else {
            buildJsonResponse(503, errorEnvelope("ACCESSIBILITY_UNAVAILABLE", "Global action '$actionName' failed. Accessibility service may not be connected."))
        }
    }
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
