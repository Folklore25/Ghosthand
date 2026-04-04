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
        previewWidth: Int,
        previewHeight: Int
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
            focusedEditablePresent = null,
            visualAvailable = screenshotResult.available,
            previewAvailable = screenshotResult.available,
            previewPath = ScreenPreviewMetadata.previewPath(
                screenshotUsableNow = screenshotResult.available,
                previewWidth = previewWidth,
                previewHeight = previewHeight
            ),
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
