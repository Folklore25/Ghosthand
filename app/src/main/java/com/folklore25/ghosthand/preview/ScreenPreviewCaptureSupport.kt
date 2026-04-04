/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.preview

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.hasUsableImage
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ScreenPreviewRequest(
    val width: Int,
    val height: Int
)

data class ScreenPreviewCapture(
    val request: ScreenPreviewRequest,
    val result: ScreenshotDispatchResult
)

object ScreenPreviewCaptureSupport {
    const val DECISION_USABLE_SHORT_EDGE = 360
    const val LIGHTWEIGHT_LONG_EDGE_CAP = 1080

    fun previewRequestForDisplay(displayWidth: Int, displayHeight: Int): ScreenPreviewRequest {
        val safeWidth = displayWidth.coerceAtLeast(1)
        val safeHeight = displayHeight.coerceAtLeast(1)
        val shorterEdge = min(safeWidth, safeHeight)
        val longerEdge = max(safeWidth, safeHeight)
        val aspectRatio = longerEdge.toDouble() / shorterEdge.toDouble()
        val capFriendlyAspectRatio = LIGHTWEIGHT_LONG_EDGE_CAP.toDouble() / DECISION_USABLE_SHORT_EDGE.toDouble()

        val scale = when {
            shorterEdge <= DECISION_USABLE_SHORT_EDGE && longerEdge <= LIGHTWEIGHT_LONG_EDGE_CAP -> 1.0
            aspectRatio <= capFriendlyAspectRatio -> DECISION_USABLE_SHORT_EDGE.toDouble() / shorterEdge.toDouble()
            else -> LIGHTWEIGHT_LONG_EDGE_CAP.toDouble() / longerEdge.toDouble()
        }

        val scaledWidth = (safeWidth * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (safeHeight * scale).roundToInt().coerceAtLeast(1)
        return ScreenPreviewRequest(
            width = scaledWidth,
            height = scaledHeight
        )
    }

    fun previewRequestForScreenshot(screenshotResult: ScreenshotDispatchResult): ScreenPreviewRequest? {
        if (!screenshotResult.hasUsableImage) {
            return null
        }
        return previewRequestForDisplay(
            displayWidth = screenshotResult.width,
            displayHeight = screenshotResult.height
        )
    }

    fun capturePreview(
        displayWidth: Int,
        displayHeight: Int,
        captureScreenshot: (Int, Int) -> ScreenshotDispatchResult
    ): ScreenPreviewCapture {
        val request = previewRequestForDisplay(
            displayWidth = displayWidth,
            displayHeight = displayHeight
        )
        return ScreenPreviewCapture(
            request = request,
            result = captureScreenshot(request.width, request.height)
        )
    }

    fun withPreviewMetadata(
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
}
