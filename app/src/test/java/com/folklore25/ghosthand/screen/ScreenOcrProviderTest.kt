/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen

import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
