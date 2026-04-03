/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ForegroundAppSnapshot
import com.folklore25.ghosthand.ScreenshotDispatchResult
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.preview.ScreenPreviewMetadata
object ScreenReadPayloadComposer {
    fun createAccessibilityPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        return GhosthandApiPayloads.accessibilityScreenRead(
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
        previewToken: String?,
        previewWidth: Int,
        previewHeight: Int
    ): ScreenReadPayload {
        return ScreenPreviewMetadata.apply(
            payload = payload,
            screenshotUsableNow = screenshotUsableNow,
            previewToken = previewToken,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun createOcrPayload(
        screenshotResult: ScreenshotDispatchResult,
        foregroundSnapshot: ForegroundAppSnapshot,
        ocrResult: ScreenOcrResult,
        previewWidth: Int,
        previewHeight: Int
    ): ScreenReadPayload {
        return ScreenReadPayload(
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
            visualAvailable = screenshotResult.available,
            previewAvailable = screenshotResult.available,
            previewToken = foregroundSnapshot.packageName?.let {
                "preview:$it:${foregroundSnapshot.activity ?: "unknown"}"
            },
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
            visualAvailable = ocrPayload.visualAvailable ?: accessibilityPayload.visualAvailable,
            previewAvailable = ocrPayload.previewAvailable ?: accessibilityPayload.previewAvailable,
            previewToken = accessibilityPayload.previewToken ?: ocrPayload.previewToken,
            previewWidth = accessibilityPayload.previewWidth ?: ocrPayload.previewWidth,
            previewHeight = accessibilityPayload.previewHeight ?: ocrPayload.previewHeight
        )
    }
}
