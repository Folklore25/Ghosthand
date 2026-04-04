/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.TestFileSupport
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
}
