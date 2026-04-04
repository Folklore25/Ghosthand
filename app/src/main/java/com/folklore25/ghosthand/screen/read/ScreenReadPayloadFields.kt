/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

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

object ScreenReadPayloadFields {
    fun screenReadFields(payload: ScreenReadPayload): Map<String, Any?> {
        val legibility = ScreenStateLegibilityProjector.fromPayload(payload)
        return linkedMapOf<String, Any?>().apply {
            putAll(surfaceContextFields(payload))
            putAll(surfaceObservationFields(payload, legibility))
            putAll(surfaceFallbackFields(payload))
            putAll(surfacePreviewFields(payload, legibility))
            putAll(
                linkedMapOf(
                    "omittedInvalidBoundsCount" to payload.omittedInvalidBoundsCount,
                    "omittedLowSignalCount" to payload.omittedLowSignalCount,
                    "omittedNodeCount" to payload.omittedNodeCount,
                    "omittedCategories" to payload.omittedCategories,
                    "omittedSummary" to payload.omittedSummary,
                    "invalidBoundsPresent" to payload.invalidBoundsPresent,
                    "lowSignalPresent" to payload.lowSignalPresent,
                    "elements" to payload.elements.map { element ->
                        linkedMapOf(
                            "nodeId" to element.nodeId,
                            "text" to element.text,
                            "desc" to element.desc,
                            "id" to element.id,
                            "clickable" to element.clickable,
                            "editable" to element.editable,
                            "focused" to element.focused,
                            "scrollable" to element.scrollable,
                            "bounds" to element.bounds,
                            "centerX" to element.centerX,
                            "centerY" to element.centerY,
                            "source" to element.source
                        )
                    }
                )
            )
        }
    }

    fun surfaceContextFields(payload: ScreenReadPayload): Map<String, Any?> {
        return linkedMapOf(
            "packageName" to payload.packageName,
            "activity" to payload.activity,
            "snapshotToken" to payload.snapshotToken,
            "capturedAt" to payload.capturedAt,
            "foregroundStableDuringCapture" to payload.foregroundStableDuringCapture
        )
    }

    fun surfaceObservationFields(payload: ScreenReadPayload): Map<String, Any?> {
        val legibility = ScreenStateLegibilityProjector.fromPayload(payload)
        return surfaceObservationFields(payload, legibility)
    }

    fun surfaceObservationFields(
        payload: ScreenReadPayload,
        legibility: ScreenStateLegibility
    ): Map<String, Any?> {
        return linkedMapOf(
            "partialOutput" to payload.partialOutput,
            "candidateNodeCount" to payload.candidateNodeCount,
            "returnedElementCount" to payload.returnedElementCount,
            "warnings" to payload.warnings,
            "source" to payload.source,
            "focusedEditablePresent" to legibility.focusedEditablePresent,
            "renderMode" to legibility.renderMode.wireValue,
            "surfaceReadability" to legibility.surfaceReadability.wireValue,
            "visualAvailable" to legibility.visualAvailable,
            "accessibilityElementCount" to payload.accessibilityElementCount,
            "ocrElementCount" to payload.ocrElementCount,
            "usedOcrFallback" to payload.usedOcrFallback
        )
    }

    fun surfaceFallbackFields(payload: ScreenReadPayload): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            payload.retryHint?.let { hint ->
                put("suggestedSource", hint.source)
                put("fallbackReason", hint.reason)
            }
        }
    }

    fun surfacePreviewFields(
        payload: ScreenReadPayload,
        legibility: ScreenStateLegibility = ScreenStateLegibilityProjector.fromPayload(payload)
    ): Map<String, Any?> {
        return linkedMapOf(
            "previewAvailable" to legibility.previewAvailable,
            "previewPath" to payload.previewPath,
            "previewWidth" to payload.previewWidth,
            "previewHeight" to payload.previewHeight
        )
    }
}
