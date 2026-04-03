/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen

import com.folklore25.ghosthand.ScreenOcrEngine
import com.folklore25.ghosthand.ScreenOcrProvider
import com.folklore25.ghosthand.ScreenshotDispatchResult
import com.folklore25.ghosthand.screen.read.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOcrProviderTest {
    @Test
    fun providerReturnsEngineElementsWhenScreenshotIsAvailable() {
        val provider = ScreenOcrProvider(
            engine = ScreenOcrEngine {
                ScreenOcrResult(
                    attemptedPath = "ocr_text_recognition",
                    elements = listOf(
                        ScreenReadElement(
                            text = "Hello",
                            bounds = "[0,0][10,10]",
                            centerX = 5,
                            centerY = 5,
                            source = ScreenReadMode.OCR.wireValue
                        )
                    )
                )
            }
        )

        val result = provider.read(
            ScreenshotDispatchResult(
                available = true,
                base64 = "ZmFrZQ==",
                format = "png",
                width = 10,
                height = 10,
                attemptedPath = "accessibility_screenshot"
            )
        )

        assertEquals("ocr_text_recognition", result.attemptedPath)
        assertEquals(1, result.elements.size)
        assertEquals(ScreenReadMode.OCR.wireValue, result.elements.first().source)
    }

    @Test
    fun providerSurfacesUnavailableScreenshotTruth() {
        val provider = ScreenOcrProvider(
            engine = ScreenOcrEngine {
                throw AssertionError("engine should not run when screenshot is unavailable")
            }
        )

        val result = provider.read(
            ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "service_missing"
            )
        )

        assertEquals("service_missing", result.attemptedPath)
        assertTrue(result.warnings.contains("ocr_screenshot_unavailable"))
        assertTrue(result.elements.isEmpty())
    }
}
