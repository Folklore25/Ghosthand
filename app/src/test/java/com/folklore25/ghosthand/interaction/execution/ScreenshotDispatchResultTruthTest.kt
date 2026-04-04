/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotDispatchResultTruthTest {
    @Test
    fun usableScreenshotTruthRequiresPositiveDimensionsAndDecodableNonEmptyImageBytes() {
        val usable = ScreenshotDispatchResult(
            available = true,
            base64 = "cG5n",
            format = "png",
            width = 1080,
            height = 2400,
            attemptedPath = "mediaprojection_capture"
        )
        val blankBytes = usable.copy(base64 = "")
        val invalidBase64 = usable.copy(base64 = "not-base64")
        val zeroWidth = usable.copy(width = 0)
        val unavailable = usable.copy(available = false)

        assertTrue(usable.hasUsableImage)
        assertFalse(blankBytes.hasUsableImage)
        assertFalse(invalidBase64.hasUsableImage)
        assertFalse(zeroWidth.hasUsableImage)
        assertFalse(unavailable.hasUsableImage)
    }

    @Test
    fun blankAccessibilityImageMustNotBlockUsableProjectionCapture() {
        val service = FakeExecutionCore(
            screenshotResult = ScreenshotDispatchResult(
                available = true,
                base64 = "",
                format = "png",
                width = 1080,
                height = 2400,
                attemptedPath = "accessibility_screenshot"
            )
        )
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(service)

        try {
            val projectionResult = ScreenshotDispatchResult(
                available = true,
                base64 = "cHJvamVjdGlvbg==",
                format = "png",
                width = 1080,
                height = 2400,
                attemptedPath = "mediaprojection_capture"
            )

            val result = AccessibilityScreenshotAccess.captureBestAvailable(
                width = 0,
                height = 0,
                captureProjection = { _, _ -> projectionResult }
            )

            assertEquals("mediaprojection_capture", result.attemptedPath)
            assertEquals("cHJvamVjdGlvbg==", result.base64)
        } finally {
            GhostAccessibilityExecutionCoreRegistry.unregister(service)
        }
    }

    @Test
    fun zeroDimensionAccessibilityImageMustNotBlockUsableProjectionCapture() {
        val service = FakeExecutionCore(
            screenshotResult = ScreenshotDispatchResult(
                available = true,
                base64 = "YWNjZXNzaWJpbGl0eQ==",
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "accessibility_screenshot"
            )
        )
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(service)

        try {
            val projectionResult = ScreenshotDispatchResult(
                available = true,
                base64 = "cHJvamVjdGlvbg==",
                format = "png",
                width = 1080,
                height = 2400,
                attemptedPath = "mediaprojection_capture"
            )

            val result = AccessibilityScreenshotAccess.captureBestAvailable(
                width = 0,
                height = 0,
                captureProjection = { _, _ -> projectionResult }
            )

            assertEquals("mediaprojection_capture", result.attemptedPath)
            assertEquals(1080, result.width)
            assertEquals(2400, result.height)
        } finally {
            GhostAccessibilityExecutionCoreRegistry.unregister(service)
        }
    }

    @Test
    fun invalidBase64AccessibilityImageMustNotBlockUsableProjectionCapture() {
        val service = FakeExecutionCore(
            screenshotResult = ScreenshotDispatchResult(
                available = true,
                base64 = "not-base64",
                format = "png",
                width = 1080,
                height = 2400,
                attemptedPath = "accessibility_screenshot"
            )
        )
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(service)

        try {
            val projectionResult = ScreenshotDispatchResult(
                available = true,
                base64 = "cHJvamVjdGlvbg==",
                format = "png",
                width = 1080,
                height = 2400,
                attemptedPath = "mediaprojection_capture"
            )

            val result = AccessibilityScreenshotAccess.captureBestAvailable(
                width = 0,
                height = 0,
                captureProjection = { _, _ -> projectionResult }
            )

            assertEquals("mediaprojection_capture", result.attemptedPath)
            assertEquals("cHJvamVjdGlvbg==", result.base64)
        } finally {
            GhostAccessibilityExecutionCoreRegistry.unregister(service)
        }
    }

    @Test
    fun customSizeRequestsCaptureNativeImageBeforeFallbackProjection() {
        val service = FakeExecutionCore(
            screenshotResult = ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "bitmap_prepare_failed"
            )
        )
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(service)

        var projectionWidth = -1
        var projectionHeight = -1

        try {
            AccessibilityScreenshotAccess.captureBestAvailable(
                width = 360,
                height = 804,
                captureProjection = { width, height ->
                    projectionWidth = width
                    projectionHeight = height
                    ScreenshotDispatchResult(
                        available = false,
                        base64 = null,
                        format = "png",
                        width = 0,
                        height = 0,
                        attemptedPath = "projection_missing"
                    )
                }
            )

            assertEquals(0, service.lastScreenshotWidth)
            assertEquals(0, service.lastScreenshotHeight)
            assertEquals(0, projectionWidth)
            assertEquals(0, projectionHeight)
        } finally {
            GhostAccessibilityExecutionCoreRegistry.unregister(service)
        }
    }

    private class FakeExecutionCore(
        private val screenshotResult: ScreenshotDispatchResult
    ) : GhostAccessibilityExecutionCore {
        var lastScreenshotWidth: Int = -1
        var lastScreenshotHeight: Int = -1

        override fun <T> withActiveWindowRoot(block: (AccessibilityNodeInfo) -> T): T? = null

        override fun dispatchConnectionActive(): Boolean = true

        override fun frameworkConnectionAvailable(): Boolean = true

        override fun currentConnectionIdForDispatch(): Int = 1

        override fun performNodeClick(nodeId: String): NodeClickDispatchResult {
            return NodeClickDispatchResult(nodeFound = false, performed = false, attemptedPath = "unused")
        }

        override fun performSetText(text: CharSequence): TextInputDispatchResult {
            return TextInputDispatchResult(targetFound = false, performed = false, attemptedPath = "unused")
        }

        override fun performImeEnterAction(): KeyInputDispatchResult {
            return KeyInputDispatchResult(targetFound = false, performed = false, attemptedPath = "unused")
        }

        override fun performTapGesture(x: Int, y: Int): Boolean = false

        override fun performSwipeGesture(
            fromX: Int,
            fromY: Int,
            toX: Int,
            toY: Int,
            durationMs: Long
        ): Boolean = false

        override fun performSwipeGestureDiagnostic(
            fromX: Int,
            fromY: Int,
            toX: Int,
            toY: Int,
            durationMs: Long
        ): SwipeGestureDispatchDiagnostic {
            return SwipeGestureDispatchDiagnostic(
                dispatched = false,
                callbackResult = "unused",
                completed = false
            )
        }

        override fun takeScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
            lastScreenshotWidth = width
            lastScreenshotHeight = height
            return screenshotResult
        }

        override fun setTextOnNode(nodeId: String, text: CharSequence): NodeTextDispatchResult {
            return NodeTextDispatchResult(nodeFound = false, performed = false, attemptedPath = "unused")
        }

        override fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean = false

        override fun performGesture(strokes: List<GestureStroke>): Boolean = false
    }
}
