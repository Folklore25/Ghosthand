/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONObject

internal class ActionNavigationRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun buildGlobalActionResponse(actionName: String, actionCode: Int): String {
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val initialResult = stateCoordinator.performGlobalAction(actionCode)
        val result = if (initialResult.performed) {
            initialResult.copy(
                effect = observeActionSurfaceChange(beforeSnapshot) {
                    stateCoordinator.getTreeSnapshotResult().snapshot
                }.toActionEffectObservation()
            )
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
