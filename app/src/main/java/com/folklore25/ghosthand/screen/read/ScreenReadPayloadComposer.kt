/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.state.device.ForegroundAppSnapshot
import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.hasUsableImage

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.payload.GhosthandScreenPayloads
import com.folklore25.ghosthand.preview.ScreenPreviewMetadata

object ScreenReadPayloadComposer {
    fun createAccessibilityPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        return GhosthandScreenPayloads.accessibilityScreenRead(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    }

    fun attachPreviewMetadata(
        payload: ScreenReadPayload,
        screenshotUsableNow: Boolean,
        previewWidth: Int?,
        previewHeight: Int?
    ): ScreenReadPayload {
        return ScreenPreviewMetadata.apply(
            payload = payload,
            screenshotUsableNow = screenshotUsableNow,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun createOcrPayload(
        screenshotResult: ScreenshotDispatchResult,
        foregroundSnapshot: ForegroundAppSnapshot,
        ocrResult: ScreenOcrResult,
        previewWidth: Int?,
        previewHeight: Int?
    ): ScreenReadPayload {
        return ScreenPreviewMetadata.apply(
            payload = ScreenReadPayload(
                packageName = foregroundSnapshot.packageName,
                activity = foregroundSnapshot.activity,
                snapshotToken = null,
                capturedAt = null,
                foregroundStableDuringCapture = true,
                partialOutput = false,
                candidateNodeCount = 0,
                returnedElementCount = ocrResult.elements.size,
                warnings = ocrResult.warnings,
                omittedInvalidBoundsCount = 0,
                omittedLowSignalCount = 0,
                omittedNodeCount = 0,
                omittedCategories = emptyList(),
                omittedSummary = null,
                invalidBoundsPresent = false,
                lowSignalPresent = false,
                elements = ocrResult.elements,
                source = ScreenReadMode.OCR.wireValue,
                accessibilityElementCount = 0,
                ocrElementCount = ocrResult.elements.size,
                usedOcrFallback = false,
                focusedEditablePresent = null,
                visualAvailable = false,
                previewAvailable = false,
                previewPath = null,
                previewWidth = null,
                previewHeight = null
            ),
            screenshotUsableNow = screenshotResult.hasUsableImage,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun createHybridPayload(
        accessibilityPayload: ScreenReadPayload,
        ocrPayload: ScreenReadPayload
    ): ScreenReadPayload {
        return mergeHybridPayloads(accessibilityPayload, ocrPayload)
    }

    fun mergeHybridPayloads(
        accessibilityPayload: ScreenReadPayload,
        ocrPayload: ScreenReadPayload
    ): ScreenReadPayload {
        if (ocrPayload.elements.isEmpty()) {
            return accessibilityPayload.copy(
                warnings = (accessibilityPayload.warnings + ocrPayload.warnings).distinct()
            )
        }

        return accessibilityPayload.copy(
            returnedElementCount = accessibilityPayload.elements.size + ocrPayload.elements.size,
            warnings = (accessibilityPayload.warnings + listOf("ocr_fallback_used") + ocrPayload.warnings).distinct(),
            elements = accessibilityPayload.elements + ocrPayload.elements,
            source = ScreenReadMode.HYBRID.wireValue,
            ocrElementCount = ocrPayload.ocrElementCount,
            usedOcrFallback = true,
            focusedEditablePresent = accessibilityPayload.focusedEditablePresent ?: ocrPayload.focusedEditablePresent,
            visualAvailable = ocrPayload.visualAvailable ?: accessibilityPayload.visualAvailable,
            previewAvailable = ocrPayload.previewAvailable ?: accessibilityPayload.previewAvailable,
            previewPath = accessibilityPayload.previewPath ?: ocrPayload.previewPath,
            previewWidth = accessibilityPayload.previewWidth ?: ocrPayload.previewWidth,
            previewHeight = accessibilityPayload.previewHeight ?: ocrPayload.previewHeight
        )
    }
}
