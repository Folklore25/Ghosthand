/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.TestFileSupport
import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadScreenshotRouteHandlersTest {
    @Test
    fun screenshotRouteDoesNotTreatAvailableAloneAsSuccess() {
        val handlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt"
        )

        assertFalse(handlers.contains("return if (screenshotResult.available)"))
    }

    @Test
    fun screenshotRouteDoesNotSerializeBlankBase64IntoSuccessPayload() {
        val handlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt"
        )

        assertFalse(handlers.contains("data:image/png;base64,\${screenshotResult.base64 ?: \"\"}"))
    }

    @Test
    fun screenshotRouteDefaultsToFullResolutionIntent() {
        val handlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt"
        )

        assertTrue(handlers.contains("val width = queryParameters[\"width\"]?.toIntOrNull() ?: 0"))
        assertTrue(handlers.contains("val height = queryParameters[\"height\"]?.toIntOrNull() ?: 0"))
    }

    @Test
    fun screenshotRouteClassifiesKnownFailuresSpecifically() {
        assertEquals(
            "INVALID_SCREENSHOT_DIMENSIONS",
            failureFor("invalid_resize_request").code
        )
        assertEquals(
            "SCREENSHOT_UNAVAILABLE",
            failureFor("service_missing").code
        )
        assertEquals(
            "SCREENSHOT_PROJECTION_UNAVAILABLE",
            failureFor("projection_missing").code
        )
        assertEquals(
            "SCREENSHOT_ACTIVITY_UNAVAILABLE",
            failureFor("root_unavailable").code
        )
        assertEquals(
            "SCREENSHOT_FRAME_TIMEOUT",
            failureFor("image_acquire_timeout").code
        )
        assertEquals(
            "SCREENSHOT_ENCODE_FAILED",
            failureFor("bitmap_prepare_failed").code
        )
        assertEquals(
            "SCREENSHOT_EMPTY_OUTPUT",
            failureFor("empty_encoded_image").code
        )
    }

    @Test
    fun screenshotRouteFallsBackToGenericCodeOnlyForUnknownFailures() {
        val failure = failureFor("unexpected_backend_state")

        assertEquals("SCREENSHOT_CAPTURE_FAILED", failure.code)
        assertEquals("Screenshot capture failed. Reason: unexpected_backend_state", failure.message)
    }

    @Test
    fun screenshotCatalogDescriptionMatchesBoundedFailureSemantics() {
        val catalog = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt",
            "src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt"
        )

        assertTrue(catalog.contains("invalid dimensions"))
        assertTrue(catalog.contains("frame timeouts"))
        assertTrue(catalog.contains("empty output"))
    }

    private fun failureFor(attemptedPath: String): ScreenshotFailureClassification {
        return ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = "png",
            width = 0,
            height = 0,
            attemptedPath = attemptedPath
        ).classifyFailure()
    }
}
