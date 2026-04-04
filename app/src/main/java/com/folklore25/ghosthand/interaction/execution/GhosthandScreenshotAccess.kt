/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import com.folklore25.ghosthand.R


internal interface GhosthandScreenshotAccess {
    fun captureBestAvailable(
        width: Int,
        height: Int,
        captureProjection: (Int, Int) -> ScreenshotDispatchResult
    ): ScreenshotDispatchResult
}

internal object AccessibilityScreenshotAccess : GhosthandScreenshotAccess {
    override fun captureBestAvailable(
        width: Int,
        height: Int,
        captureProjection: (Int, Int) -> ScreenshotDispatchResult
    ): ScreenshotDispatchResult {
        val serviceCapture = takeAccessibilityScreenshot(width, height)
        if (serviceCapture.hasUsableImage) {
            return serviceCapture
        }

        val projectionCapture = captureProjection(width, height)
        if (projectionCapture.hasUsableImage) {
            return projectionCapture
        }

        return if (projectionCapture.attemptedPath != "projection_missing") {
            projectionCapture
        } else if (serviceCapture.attemptedPath != "service_disconnected") {
            serviceCapture
        } else {
            projectionCapture
        }
    }

    private fun takeAccessibilityScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "service_missing"
            )
        return service.takeScreenshot(width, height)
    }
}
