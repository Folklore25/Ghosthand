/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateCoordinatorScreenPayloadTest {
    @Test
    fun screenReadFieldsExposeSourceAwareHybridPayload() {
        val payload = GhosthandApiPayloads.screenReadFields(
            ScreenReadPayload(
                packageName = "com.example",
                activity = "ExampleActivity",
                snapshotToken = "snap",
                capturedAt = "2026-04-01T00:00:00Z",
                foregroundStableDuringCapture = true,
                partialOutput = true,
                candidateNodeCount = 1,
                returnedElementCount = 2,
                warnings = listOf("partial_output", "ocr_fallback_used"),
                omittedInvalidBoundsCount = 0,
                omittedLowSignalCount = 0,
                omittedNodeCount = 0,
                omittedCategories = emptyList(),
                omittedSummary = null,
                invalidBoundsPresent = false,
                lowSignalPresent = false,
                elements = listOf(
                    ScreenReadElement(
                        nodeId = "p0.0@tsnap",
                        text = "Settings",
                        bounds = "[0,0][20,20]",
                        centerX = 10,
                        centerY = 10,
                        source = ScreenReadMode.ACCESSIBILITY.wireValue
                    ),
                    ScreenReadElement(
                        text = "Welcome",
                        bounds = "[20,20][60,40]",
                        centerX = 40,
                        centerY = 30,
                        source = ScreenReadMode.OCR.wireValue
                    )
                ),
                source = ScreenReadMode.HYBRID.wireValue,
                accessibilityElementCount = 1,
                ocrElementCount = 1,
                usedOcrFallback = true,
                retryHint = null
            )
        )

        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["source"])
        assertEquals(1, payload["accessibilityElementCount"])
        assertEquals(1, payload["ocrElementCount"])
        assertEquals(true, payload["usedOcrFallback"])
        assertEquals(emptyList<String>(), payload["omittedCategories"])
        assertEquals(null, payload["omittedSummary"])
        val elements = payload["elements"] as List<*>
        assertEquals(ScreenReadMode.ACCESSIBILITY.wireValue, (elements[0] as Map<*, *>)["source"])
        assertEquals(ScreenReadMode.OCR.wireValue, (elements[1] as Map<*, *>)["source"])
        assertTrue((payload["warnings"] as List<*>).contains("ocr_fallback_used"))
        assertNull(payload["retryHint"])
    }

    @Test
    fun screenReadFieldsExposeRetryHintForAccessibilityFallback() {
        val payload = GhosthandApiPayloads.screenReadFields(
            ScreenReadPayload(
                packageName = "com.example",
                activity = "ExampleActivity",
                snapshotToken = "snap",
                capturedAt = "2026-04-01T00:00:00Z",
                foregroundStableDuringCapture = true,
                partialOutput = true,
                candidateNodeCount = 3,
                returnedElementCount = 1,
                warnings = listOf("partial_output"),
                omittedInvalidBoundsCount = 0,
                omittedLowSignalCount = 2,
                omittedNodeCount = 2,
                omittedCategories = listOf("low_signal"),
                omittedSummary = "Omitted 2 low-signal nodes.",
                invalidBoundsPresent = false,
                lowSignalPresent = true,
                elements = listOf(
                    ScreenReadElement(
                        nodeId = "p0.0@tsnap",
                        text = "Only visible node",
                        bounds = "[0,0][20,20]",
                        centerX = 10,
                        centerY = 10,
                        source = ScreenReadMode.ACCESSIBILITY.wireValue
                    )
                ),
                source = ScreenReadMode.ACCESSIBILITY.wireValue,
                accessibilityElementCount = 1,
                ocrElementCount = 0,
                usedOcrFallback = false,
                retryHint = ScreenReadRetryHint(
                    source = ScreenReadMode.HYBRID.wireValue,
                    reason = "accessibility_operationally_insufficient"
                )
            )
        )

        val retryHint = payload["retryHint"] as Map<*, *>
        assertEquals(ScreenReadMode.HYBRID.wireValue, retryHint["source"])
        assertEquals("accessibility_operationally_insufficient", retryHint["reason"])
    }
}
