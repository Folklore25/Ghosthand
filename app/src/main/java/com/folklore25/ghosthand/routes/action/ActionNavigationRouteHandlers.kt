/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
