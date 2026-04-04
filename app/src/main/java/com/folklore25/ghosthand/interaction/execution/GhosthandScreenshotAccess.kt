/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import com.folklore25.ghosthand.R

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

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
        if (width > 0 && height > 0) {
            return captureBestResized(
                width = width,
                height = height,
                captureProjection = captureProjection
            )
        }

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

    private fun captureBestResized(
        width: Int,
        height: Int,
        captureProjection: (Int, Int) -> ScreenshotDispatchResult
    ): ScreenshotDispatchResult {
        val serviceCapture = takeAccessibilityScreenshot(0, 0)
        val resizedServiceCapture = serviceCapture.resizeTo(width, height)
        if (resizedServiceCapture.hasUsableImage) {
            return resizedServiceCapture
        }

        val projectionCapture = captureProjection(0, 0)
        val resizedProjectionCapture = projectionCapture.resizeTo(width, height)
        if (resizedProjectionCapture.hasUsableImage) {
            return resizedProjectionCapture
        }

        return if (projectionCapture.attemptedPath != "projection_missing") {
            resizedProjectionCapture
        } else if (serviceCapture.attemptedPath != "service_disconnected") {
            resizedServiceCapture
        } else {
            resizedProjectionCapture
        }
    }

    private fun ScreenshotDispatchResult.resizeTo(
        width: Int,
        height: Int
    ): ScreenshotDispatchResult {
        if (!hasUsableImage) {
            return this
        }
        if (this.width == width && this.height == height) {
            return this
        }

        return try {
            val sourceBytes = encodedBytes ?: return failedResizeResult()
            val sourceBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
                ?: return failedResizeResult()
            val resizedBitmap = try {
                Bitmap.createScaledBitmap(sourceBitmap, width, height, true)
            } finally {
                if (!sourceBitmap.isRecycled) {
                    sourceBitmap.recycle()
                }
            }

            try {
                val output = java.io.ByteArrayOutputStream()
                val encoded = if (resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, output)) {
                    output.toByteArray()
                } else {
                    ByteArray(0)
                }
                val resized = ScreenshotDispatchResult(
                    available = true,
                    base64 = Base64.encodeToString(encoded, Base64.NO_WRAP),
                    format = format,
                    width = resizedBitmap.width,
                    height = resizedBitmap.height,
                    attemptedPath = "${attemptedPath}_resized"
                )
                if (resized.hasUsableImage) {
                    resized
                } else {
                    failedResizeResult()
                }
            } finally {
                if (!resizedBitmap.isRecycled) {
                    resizedBitmap.recycle()
                }
            }
        } catch (_: Exception) {
            failedResizeResult()
        }
    }

    private fun ScreenshotDispatchResult.failedResizeResult(): ScreenshotDispatchResult =
        ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = format,
            width = 0,
            height = 0,
            attemptedPath = "resize_encode_failed"
        )
}
