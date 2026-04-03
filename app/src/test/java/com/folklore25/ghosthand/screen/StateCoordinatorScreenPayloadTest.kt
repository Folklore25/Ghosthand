/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import com.folklore25.ghosthand.preview.ScreenPreviewMetadata
import com.folklore25.ghosthand.screen.read.ScreenReadPayloadComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateCoordinatorScreenPayloadTest {
    @Test
    fun hybridSupportMergesOcrFallbackWarningsAndPreviewMetadata() {
        val accessibilityPayload = ScreenReadPayload(
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
            omittedLowSignalCount = 1,
            omittedNodeCount = 1,
            omittedCategories = listOf("low_signal"),
            omittedSummary = "Omitted 1 low-signal node.",
            invalidBoundsPresent = false,
            lowSignalPresent = true,
            elements = listOf(
                ScreenReadElement(
                    nodeId = "p0.0@tsnap",
                    text = "Accessibility node",
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
            visualAvailable = false,
            previewAvailable = false,
            previewToken = null,
            previewWidth = null,
            previewHeight = null,
            retryHint = null
        )
        val ocrPayload = ScreenReadPayload(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = null,
            capturedAt = null,
            foregroundStableDuringCapture = true,
            partialOutput = false,
            candidateNodeCount = 0,
            returnedElementCount = 1,
            warnings = listOf("ocr_warning"),
            omittedInvalidBoundsCount = 0,
            omittedLowSignalCount = 0,
            omittedNodeCount = 0,
            omittedCategories = emptyList(),
            omittedSummary = null,
            invalidBoundsPresent = false,
            lowSignalPresent = false,
            elements = listOf(
                ScreenReadElement(
                    text = "OCR node",
                    bounds = "[20,20][60,40]",
                    centerX = 40,
                    centerY = 30,
                    source = ScreenReadMode.OCR.wireValue
                )
            ),
            source = ScreenReadMode.OCR.wireValue,
            accessibilityElementCount = 0,
            ocrElementCount = 1,
            usedOcrFallback = false,
            visualAvailable = true,
            previewAvailable = true,
            previewToken = "preview:ocr",
            previewWidth = 240,
            previewHeight = 240,
            retryHint = null
        )

        val merged = ScreenReadPayloadComposer.mergeHybridPayloads(
            accessibilityPayload = accessibilityPayload,
            ocrPayload = ocrPayload
        )

        assertEquals(ScreenReadMode.HYBRID.wireValue, merged.source)
        assertEquals(2, merged.returnedElementCount)
        assertEquals(1, merged.ocrElementCount)
        assertEquals(true, merged.usedOcrFallback)
        assertEquals("preview:ocr", merged.previewToken)
        assertTrue(merged.warnings.contains("ocr_fallback_used"))
        assertTrue(merged.warnings.contains("ocr_warning"))
    }

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
                visualAvailable = true,
                retryHint = null
            )
        )

        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["source"])
        assertEquals("hybrid", payload["renderMode"])
        assertEquals("limited", payload["surfaceReadability"])
        assertEquals(true, payload["visualAvailable"])
        assertEquals(null, payload["previewImage"])
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
                visualAvailable = true,
                retryHint = ScreenReadRetryHint(
                    source = ScreenReadMode.HYBRID.wireValue,
                    reason = "accessibility_operationally_insufficient"
                )
            )
        )

        val retryHint = payload["retryHint"] as Map<*, *>
        assertEquals("limited_accessibility", payload["renderMode"])
        assertEquals("limited", payload["surfaceReadability"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, retryHint["source"])
        assertEquals("accessibility_operationally_insufficient", retryHint["reason"])
    }

    @Test
    fun screenSummaryFieldsExposeFocusedEditableWithoutElements() {
        val payload = GhosthandApiPayloads.screenSummaryFields(
            ScreenReadPayload(
                packageName = "com.example",
                activity = "EditorActivity",
                snapshotToken = "snap",
                capturedAt = "2026-04-01T00:00:00Z",
                foregroundStableDuringCapture = true,
                partialOutput = false,
                candidateNodeCount = 2,
                returnedElementCount = 2,
                warnings = emptyList(),
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
                        text = "Editor",
                        editable = true,
                        focused = true,
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
                retryHint = null
            )
        )

        assertEquals(true, payload["focusedEditablePresent"])
        assertNull(payload["elements"])
    }

    @Test
    fun screenReadFieldsExposePreviewMetadataWhenAvailable() {
        val payload = GhosthandApiPayloads.screenReadFields(
            ScreenReadPayload(
                packageName = "com.example",
                activity = "PreviewActivity",
                snapshotToken = "snap",
                capturedAt = "2026-04-01T00:00:00Z",
                foregroundStableDuringCapture = true,
                partialOutput = false,
                candidateNodeCount = 1,
                returnedElementCount = 1,
                warnings = emptyList(),
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
                        text = "Preview",
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
                visualAvailable = true,
                previewAvailable = true,
                previewToken = "preview:snap",
                previewWidth = 240,
                previewHeight = 240,
                previewImage = "data:image/png;base64,thumb",
                retryHint = null
            )
        )

        assertEquals(true, payload["previewAvailable"])
        assertEquals("preview:snap", payload["previewToken"])
        assertEquals(240, payload["previewWidth"])
        assertEquals(240, payload["previewHeight"])
        assertEquals("data:image/png;base64,thumb", payload["previewImage"])
    }

    @Test
    fun previewMetadataSupportExposesPreviewAvailabilityInDedicatedPreviewModule() {
        val payload = ScreenPreviewMetadata.apply(
            payload = ScreenReadPayload(
                packageName = "com.example",
                activity = "ExampleActivity",
                snapshotToken = "snap",
                capturedAt = "2026-04-01T00:00:00Z",
                foregroundStableDuringCapture = true,
                partialOutput = false,
                candidateNodeCount = 1,
                returnedElementCount = 1,
                warnings = emptyList(),
                omittedInvalidBoundsCount = 0,
                omittedLowSignalCount = 0,
                omittedNodeCount = 0,
                omittedCategories = emptyList(),
                omittedSummary = null,
                invalidBoundsPresent = false,
                lowSignalPresent = false,
                elements = emptyList(),
                source = ScreenReadMode.ACCESSIBILITY.wireValue,
                accessibilityElementCount = 1,
                ocrElementCount = 0,
                usedOcrFallback = false
            ),
            screenshotUsableNow = true,
            previewToken = "preview:snap",
            previewWidth = 240,
            previewHeight = 240
        )

        assertEquals(true, payload.visualAvailable)
        assertEquals(true, payload.previewAvailable)
        assertEquals("preview:snap", payload.previewToken)
    }

    @Test
    fun coordinatorDelegatesScreenReadCompositionToDomainModules() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/StateCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val screenReadPayloadComposer = ScreenReadPayloadComposer"))
        assertTrue(coordinator.contains("private val screenPreviewMetadata = ScreenPreviewMetadata"))
        assertTrue(coordinator.contains("screenPreviewMetadata.apply("))
        assertTrue(coordinator.contains("screenReadPayloadComposer.createOcrPayload("))
        assertTrue(coordinator.contains("screenReadPayloadComposer.createHybridPayload("))
        assertTrue(coordinator.contains("screenReadPayloadComposer.createAccessibilityPayload("))
    }
}
