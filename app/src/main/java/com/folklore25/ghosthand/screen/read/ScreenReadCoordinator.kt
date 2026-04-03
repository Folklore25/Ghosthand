/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.CapabilityAccessSnapshot
import com.folklore25.ghosthand.ForegroundAppSnapshot
import com.folklore25.ghosthand.ScreenOcrProvider
import com.folklore25.ghosthand.ScreenshotDispatchResult
import com.folklore25.ghosthand.preview.ScreenPreviewCaptureSupport

internal class ScreenReadCoordinator(
    private val capabilityAccessSnapshotProvider: () -> CapabilityAccessSnapshot,
    private val captureScreenshot: (Int, Int) -> ScreenshotDispatchResult,
    private val foregroundSnapshotProvider: () -> ForegroundAppSnapshot,
    private val screenOcrProvider: ScreenOcrProvider,
    private val previewWidth: Int,
    private val previewHeight: Int
) {
    fun createAccessibilityPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        return ScreenPreviewCaptureSupport.withPreviewMetadata(
            payload = ScreenReadPayloadComposer.createAccessibilityPayload(
                snapshot = snapshot,
                editableOnly = editableOnly,
                scrollableOnly = scrollableOnly,
                packageFilter = packageFilter,
                clickableOnly = clickableOnly
            ),
            screenshotUsableNow = capabilityAccessSnapshotProvider().screenshot.effective.usableNow,
            previewToken = ScreenPreviewCaptureSupport.previewTokenForSnapshot(snapshot),
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun createOcrPayload(): ScreenReadPayload {
        val screenshotResult = captureScreenshot(0, 0)
        val foregroundSnapshot = foregroundSnapshotProvider()
        val ocrResult = screenOcrProvider.read(screenshotResult)

        return ScreenReadPayloadComposer.createOcrPayload(
            screenshotResult = screenshotResult,
            foregroundSnapshot = foregroundSnapshot,
            ocrResult = ocrResult,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        ).copy(
            previewToken = ScreenPreviewCaptureSupport.previewTokenForForeground(foregroundSnapshot)
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
