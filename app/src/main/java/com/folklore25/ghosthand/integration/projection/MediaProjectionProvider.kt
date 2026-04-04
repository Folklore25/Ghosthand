/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.integration.projection

import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.hasUsableImage

import com.folklore25.ghosthand.R

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Manages MediaProjection lifecycle for full-screen capture.
 *
 * MediaProjection requires:
 * - User consent via system dialog (triggered from an Activity)
 * - Ghosthand's runtime foreground service already running during capture
 *
 * Usage:
 * 1. Store a valid MediaProjection via [setProjection].
 * 2. Call [captureScreenshot] to acquire a full-screen frame.
 * 3. Call [clearProjection] to release when done.
 */
class MediaProjectionProvider(private val context: Context) {

    @Volatile
    private var mediaProjection: MediaProjection? = null

    @Volatile
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var imageReader: ImageReader? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Stores a live MediaProjection obtained from the system consent result.
     * Call this from Activity.onActivityResult after the user grants permission.
     */
    fun setProjection(projection: MediaProjection) {
        clearProjection()
        mediaProjection = projection
    }

    /**
     * Returns true if a MediaProjection is currently stored and ready.
     */
    fun hasProjection(): Boolean = mediaProjection != null

    /**
     * Releases all projection resources. Call when screenshot capability is no longer needed.
     */
    fun clearProjection() {
        try {
            virtualDisplay?.release()
        } catch (error: Exception) {
            Log.w(LOG_TAG, "component=MediaProjectionProvider operation=releaseVirtualDisplay failure=${error.javaClass.simpleName}", error)
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (error: Exception) {
            Log.w(LOG_TAG, "component=MediaProjectionProvider operation=closeImageReader failure=${error.javaClass.simpleName}", error)
        }
        imageReader = null
        try {
            mediaProjection?.stop()
        } catch (error: Exception) {
            Log.w(LOG_TAG, "component=MediaProjectionProvider operation=stopProjection failure=${error.javaClass.simpleName}", error)
        }
        mediaProjection = null
    }

    /**
     * Captures a full-screen screenshot using the stored MediaProjection.
     * Returns a ScreenshotDispatchResult with base64 PNG data.
     */
    fun captureScreenshot(requestWidth: Int, requestHeight: Int): ScreenshotDispatchResult {
        val projection = mediaProjection
            ?: return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "projection_missing"
            )

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val width = if (requestWidth > 0) requestWidth else screenWidth
        val height = if (requestHeight > 0) requestHeight else screenHeight

        if (hasInvalidResizeRequest(requestWidth, requestHeight)) {
            return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "invalid_resize_request"
            )
        }

        val displayDpi = displayMetrics.densityDpi

        val latch = java.util.concurrent.CountDownLatch(1)
        var result = ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = "png",
            width = 0,
            height = 0,
            attemptedPath = "not_dispatched"
        )

        executor.execute {
            try {
                @Suppress("DEPRECATION")
                val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
                imageReader = reader

                val vd = projection.createVirtualDisplay(
                    "GhosthandScreenshot",
                    width,
                    height,
                    displayDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                    reader.surface,
                    null,
                    null
                )
                virtualDisplay = vd

                val imageAvailableLatch = java.util.concurrent.CountDownLatch(1)
                reader.setOnImageAvailableListener(
                    { imageAvailableLatch.countDown() },
                    Handler(Looper.getMainLooper())
                )
                if (!imageAvailableLatch.await(SCREENSHOT_FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    result = ScreenshotDispatchResult(
                        available = false,
                        base64 = null,
                        format = "png",
                        width = 0,
                        height = 0,
                        attemptedPath = "image_acquire_timeout"
                    )
                    latch.countDown()
                    return@execute
                }

                val image = reader.acquireLatestImage()
                if (image == null) {
                    result = ScreenshotDispatchResult(
                        available = false,
                        base64 = null,
                        format = "png",
                        width = 0,
                        height = 0,
                        attemptedPath = "image_acquire_timeout"
                    )
                    latch.countDown()
                    return@execute
                }

                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = android.graphics.Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    val finalBitmap = if (rowPadding > 0) {
                        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    } else {
                        bitmap
                    }

                    val baos = java.io.ByteArrayOutputStream()
                    val encoded = if (finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, baos)) {
                        baos.toByteArray()
                    } else {
                        ByteArray(0)
                    }
                    val candidate = ScreenshotDispatchResult(
                        available = true,
                        base64 = Base64.encodeToString(encoded, Base64.NO_WRAP),
                        format = "png",
                        width = finalBitmap.width,
                        height = finalBitmap.height,
                        attemptedPath = "mediaprojection_capture"
                    )
                    result = if (candidate.hasUsableImage) {
                        candidate
                    } else {
                        ScreenshotDispatchResult(
                            available = false,
                            base64 = null,
                            format = "png",
                            width = 0,
                            height = 0,
                            attemptedPath = "empty_encoded_image"
                        )
                    }

                    if (finalBitmap !== bitmap) finalBitmap.recycle()
                    bitmap.recycle()
                } finally {
                    image.close()
                }
            } catch (e: Exception) {
                Log.e(
                    LOG_TAG,
                    "component=MediaProjectionProvider operation=captureScreenshot width=$width height=$height failure=${e.javaClass.simpleName}",
                    e
                )
                result = ScreenshotDispatchResult(
                    available = false,
                    base64 = null,
                    format = "png",
                    width = 0,
                    height = 0,
                    attemptedPath = "capture_exception_${e.javaClass.simpleName}"
                )
            } finally {
                try {
                    virtualDisplay?.release()
                } catch (error: Exception) {
                    Log.w(LOG_TAG, "component=MediaProjectionProvider operation=finalReleaseVirtualDisplay failure=${error.javaClass.simpleName}", error)
                }
                virtualDisplay = null
                try {
                    imageReader?.close()
                } catch (error: Exception) {
                    Log.w(LOG_TAG, "component=MediaProjectionProvider operation=finalCloseImageReader failure=${error.javaClass.simpleName}", error)
                }
                imageReader = null
                latch.countDown()
            }
        }

        latch.await(SCREENSHOT_TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    private companion object {
        private const val SCREENSHOT_FRAME_TIMEOUT_MS = 1500L
        private const val SCREENSHOT_TOTAL_TIMEOUT_MS = 5000L
        private const val LOG_TAG = "MediaProjection"
    }

    private fun hasInvalidResizeRequest(requestWidth: Int, requestHeight: Int): Boolean {
        if (requestWidth < 0 || requestHeight < 0) {
            return true
        }
        val requestsNativeSize = requestWidth == 0 && requestHeight == 0
        val requestsCustomSize = requestWidth > 0 && requestHeight > 0
        return !requestsNativeSize && !requestsCustomSize
    }
}
