/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.screen.read.TreeUnavailableReason

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.payload.GhosthandPayloadJsonSupport
import com.folklore25.ghosthand.payload.GhosthandScreenPayloads
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.buildTreeUnavailableResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONObject

internal class ReadScreenRouteHandlers(
    private val stateCoordinator: StateCoordinator,
    private val observationPublisher: GhosthandObservationPublisher
) {
    fun buildScreenResponse(queryParameters: Map<String, String>): String {
        val mode = ScreenReadMode.fromWireValue(queryParameters["source"]) ?: ScreenReadMode.ACCESSIBILITY
        val summaryOnly = screenSummaryOnlyRequested(queryParameters)
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
            observationPublisher.recordAccessibilityTemporarilyUnavailable(
                reason = treeSnapshotResult.reason,
                requestedSource = mode
            )
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
        val payload = when (mode) {
            ScreenReadMode.ACCESSIBILITY -> {
                val requiredSnapshot = snapshot
                    ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)
                stateCoordinator.createScreenReadPayload(
                    snapshot = requiredSnapshot,
                    editableOnly = editableOnly,
                    scrollableOnly = scrollableOnly,
                    packageFilter = queryParameters["package"],
                    clickableOnly = clickableOnly
                )
            }

            ScreenReadMode.OCR -> stateCoordinator.createOcrScreenPayload()
            ScreenReadMode.HYBRID -> {
                if (snapshot != null) {
                    stateCoordinator.createHybridScreenPayload(
                        snapshot = snapshot,
                        packageFilter = queryParameters["package"]
                    )
                } else {
                    stateCoordinator.createOcrScreenPayload()
                }
            }
        }

        observationPublisher.recordScreenPayload(payload)

        val bodyPayload = if (summaryOnly) {
            GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.summaryFields(payload))
        } else {
            GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.screenReadFields(payload))
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                data = bodyPayload,
                disclosure = buildScreenDisclosure(payload)
            )
        )
    }
}

internal fun screenSummaryOnlyRequested(queryParameters: Map<String, String>): Boolean {
    return queryParameters["summaryOnly"] == "true"
}

internal fun buildScreenDisclosure(payload: JSONObject): GhosthandDisclosure? {
    return buildScreenDisclosure(
        partialOutput = payload.optBoolean("partialOutput", false),
        foregroundStableDuringCapture = payload.optBoolean("foregroundStableDuringCapture", true)
    )
}

internal fun buildScreenDisclosure(payload: ScreenReadPayload): GhosthandDisclosure? {
    return buildScreenDisclosure(
        partialOutput = payload.partialOutput,
        foregroundStableDuringCapture = payload.foregroundStableDuringCapture
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
