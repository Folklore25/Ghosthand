/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.capability.CapabilityAccessSnapshot
import com.folklore25.ghosthand.state.device.ForegroundAppSnapshot
import com.folklore25.ghosthand.screen.ocr.ScreenOcrProvider
import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.hasUsableImage

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.preview.ScreenPreviewCapture
import com.folklore25.ghosthand.preview.ScreenPreviewCaptureSupport

internal class ScreenReadCoordinator(
    private val capabilityAccessSnapshotProvider: () -> CapabilityAccessSnapshot,
    private val captureScreenshot: (Int, Int) -> ScreenshotDispatchResult,
    private val capturePreview: () -> ScreenPreviewCapture,
    private val foregroundSnapshotProvider: () -> ForegroundAppSnapshot,
    private val screenOcrProvider: ScreenOcrProvider
) {
    fun createAccessibilityPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        val previewCapture = capabilityAccessSnapshotProvider()
            .screenshot
            .effective
            .usableNow
            .takeIf { it }
            ?.let { capturePreview() }
        return ScreenPreviewCaptureSupport.withPreviewMetadata(
            payload = ScreenReadPayloadComposer.createAccessibilityPayload(
                snapshot = snapshot,
                editableOnly = editableOnly,
                scrollableOnly = scrollableOnly,
                packageFilter = packageFilter,
                clickableOnly = clickableOnly
            ),
            screenshotUsableNow = previewCapture?.result?.hasUsableImage == true,
            previewWidth = previewCapture?.request?.width,
            previewHeight = previewCapture?.request?.height
        )
    }

    fun createOcrPayload(): ScreenReadPayload {
        val screenshotResult = captureScreenshot(0, 0)
        val foregroundSnapshot = foregroundSnapshotProvider()
        val ocrResult = screenOcrProvider.read(screenshotResult)
        val previewRequest = ScreenPreviewCaptureSupport.previewRequestForScreenshot(screenshotResult)

        return ScreenReadPayloadComposer.createOcrPayload(
            screenshotResult = screenshotResult,
            foregroundSnapshot = foregroundSnapshot,
            ocrResult = ocrResult,
            previewWidth = previewRequest?.width,
            previewHeight = previewRequest?.height
        )
    }

    fun createHybridPayload(
        snapshot: AccessibilityTreeSnapshot,
        packageFilter: String?
    ): ScreenReadPayload {
        val accessibilityPayload = createAccessibilityPayload(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = packageFilter,
            clickableOnly = false
        )
        if (!accessibilityPayload.accessibilityTreeIsOperationallyInsufficient()) {
            return accessibilityPayload
        }

        val ocrPayload = createOcrPayload()
        return ScreenReadPayloadComposer.createHybridPayload(
            accessibilityPayload = accessibilityPayload,
            ocrPayload = ocrPayload
        )
    }
}
