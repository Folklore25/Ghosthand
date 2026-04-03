/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

object ScreenReadPayloadFields {
    fun screenReadFields(payload: ScreenReadPayload): Map<String, Any?> {
        val legibility = ScreenStateLegibilityProjector.fromPayload(payload)
        return linkedMapOf<String, Any?>().apply {
            putAll(surfaceContextFields(payload))
            putAll(surfaceObservationFields(payload, legibility))
            putAll(surfaceFallbackFields(payload, includeRetryHint = true))
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

    fun surfaceFallbackFields(
        payload: ScreenReadPayload,
        includeRetryHint: Boolean
    ): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "suggestedFallback" to payload.retryHint?.source,
            "suggestedSource" to payload.retryHint?.source,
            "fallbackReason" to payload.retryHint?.reason
        ).apply {
            if (includeRetryHint) {
                put(
                    "retryHint",
                    payload.retryHint?.let { hint ->
                        linkedMapOf("source" to hint.source, "reason" to hint.reason)
                    }
                )
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
