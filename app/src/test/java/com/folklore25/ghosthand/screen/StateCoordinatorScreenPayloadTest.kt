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

import com.folklore25.ghosthand.TestFileSupport
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.preview.ScreenPreviewMetadata
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.ScreenSummaryPayloadComposer
import com.folklore25.ghosthand.screen.read.ScreenReadPayloadComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            previewPath = null,
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
            previewPath = "/screenshot?width=240&height=240",
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
        assertEquals("/screenshot?width=240&height=240", merged.previewPath)
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
        assertEquals(null, payload["previewPath"])
        assertFalse(payload.containsKey("previewImage"))
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

        assertEquals("limited_accessibility", payload["renderMode"])
        assertEquals("limited", payload["surfaceReadability"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["suggestedSource"])
        assertEquals("accessibility_operationally_insufficient", payload["fallbackReason"])
        assertFalse(payload.containsKey("retryHint"))
    }

    @Test
    fun screenSummaryFieldsExposeFocusedEditableWithoutElements() {
        val screenReadPayload = ScreenReadPayload(
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
            focusedEditablePresent = true,
            retryHint = null
        )
        val payload = GhosthandApiPayloads.screenSummaryFields(screenReadPayload)

        assertEquals(true, payload["focusedEditablePresent"])
        assertNull(payload["elements"])
        assertEquals(
            ScreenSummaryPayloadComposer.summaryFields(screenReadPayload),
            payload
        )
    }

    @Test
    fun screenReadFieldsProjectCanonicalFocusedEditableTruth() {
        val screenReadPayload = ScreenReadPayload(
            packageName = "com.example",
            activity = "EditorActivity",
            snapshotToken = "snap",
            capturedAt = "2026-04-01T00:00:00Z",
            foregroundStableDuringCapture = true,
            partialOutput = false,
            candidateNodeCount = 2,
            returnedElementCount = 1,
            warnings = emptyList(),
            omittedInvalidBoundsCount = 1,
            omittedLowSignalCount = 0,
            omittedNodeCount = 1,
            omittedCategories = listOf("invalid_bounds"),
            omittedSummary = "Omitted 1 invalid-bounds node.",
            invalidBoundsPresent = true,
            lowSignalPresent = false,
            elements = listOf(
                ScreenReadElement(
                    nodeId = "p0.1@tsnap",
                    text = "Submit",
                    clickable = true,
                    bounds = "[20,20][60,40]",
                    centerX = 40,
                    centerY = 30,
                    source = ScreenReadMode.ACCESSIBILITY.wireValue
                )
            ),
            source = ScreenReadMode.ACCESSIBILITY.wireValue,
            accessibilityElementCount = 1,
            ocrElementCount = 0,
            usedOcrFallback = false,
            focusedEditablePresent = true,
            retryHint = null
        )

        val fields = GhosthandApiPayloads.screenReadFields(screenReadPayload)
        val summary = GhosthandApiPayloads.screenSummaryFields(screenReadPayload)

        assertEquals(true, summary["focusedEditablePresent"])
        assertEquals(summary["focusedEditablePresent"], fields["focusedEditablePresent"])
    }

    @Test
    fun screenReadFieldsExposeExplicitPreviewPathWhenAvailable() {
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
                previewPath = "/screenshot?width=240&height=240",
                previewWidth = 240,
                previewHeight = 240,
                retryHint = null
            )
        )

        assertEquals(true, payload["previewAvailable"])
        assertEquals("/screenshot?width=240&height=240", payload["previewPath"])
        assertEquals(240, payload["previewWidth"])
        assertEquals(240, payload["previewHeight"])
        assertFalse(payload.containsKey("previewImage"))
    }

    @Test
    fun previewMetadataSupportExposesPreviewAvailabilityAndRouteInDedicatedPreviewModule() {
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
            previewWidth = 240,
            previewHeight = 240
        )

        assertEquals(true, payload.visualAvailable)
        assertEquals(true, payload.previewAvailable)
        assertEquals("/screenshot?width=240&height=240", payload.previewPath)
    }

    @Test
    fun screenPreviewContractUsesExplicitScreenshotPathInsteadOfInlineThumbs() {
        val routeHandlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenRouteHandlers.kt"
        )
        val catalogRoutes = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt",
            "src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt"
        )

        assertFalse(routeHandlers.contains("includePreview"))
        assertFalse(routeHandlers.contains("previewImage"))
        assertFalse(catalogRoutes.contains("includePreview"))
        assertTrue(catalogRoutes.contains("previewPath"))
        assertTrue(catalogRoutes.contains("/screenshot?width={previewWidth}&height={previewHeight}"))
    }

    @Test
    fun coordinatorDelegatesScreenReadCompositionToDomainModules() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val screenReadCoordinator = ScreenReadCoordinator("))
        assertTrue(coordinator.contains("screenReadCoordinator.createAccessibilityPayload("))
        assertTrue(coordinator.contains("screenReadCoordinator.createOcrPayload()"))
        assertTrue(coordinator.contains("screenReadCoordinator.createHybridPayload("))
    }

    @Test
    fun screenPayloadSupportUsesDedicatedSummaryComposer() {
        val payloadSupport = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/payload/GhosthandScreenPayloadSupport.kt",
            "src/main/java/com/folklore25/ghosthand/payload/GhosthandScreenPayloadSupport.kt"
        )

        assertTrue(payloadSupport.contains("ScreenSummaryPayloadComposer.summaryFields(payload)"))
        assertFalse(payloadSupport.contains("putAll(surfaceContextFields(payload))"))
    }

    @Test
    fun coordinatorDelegatesFindAndFocusedPayloadOwnershipToScreenFindModule() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val screenFindCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/screen/find/ScreenFindCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/screen/find/ScreenFindCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val screenFindCoordinator = ScreenFindCoordinator("))
        assertTrue(coordinator.contains("screenFindCoordinator.createFindPayload("))
        assertTrue(coordinator.contains("screenFindCoordinator.findResult("))
        assertTrue(coordinator.contains("screenFindCoordinator.getFocusedNodeResult()"))
        assertTrue(coordinator.contains("screenFindCoordinator.createFocusedNodePayload("))
        assertTrue(screenFindCoordinator.contains("fun createFindPayload("))
        assertTrue(screenFindCoordinator.contains("fun getFocusedNodeResult(): FocusedNodeResult"))
    }

    @Test
    fun coordinatorDelegatesReadAndPreviewAssemblyToDedicatedModules() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val snapshotCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenSnapshotCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/screen/read/ScreenSnapshotCoordinator.kt"
        )
        val readCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadCoordinator.kt"
        )
        val previewCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val screenSnapshotCoordinator = ScreenSnapshotCoordinator("))
        assertTrue(coordinator.contains("private val screenPreviewCoordinator = ScreenPreviewCoordinator("))
        assertTrue(coordinator.contains("screenSnapshotCoordinator.getTreeSnapshotResult()"))
        assertTrue(coordinator.contains("screenSnapshotCoordinator.createTreePayload("))
        assertTrue(coordinator.contains("screenSnapshotCoordinator.createScreenPayload("))
        assertTrue(coordinator.contains("private val screenReadCoordinator = ScreenReadCoordinator("))
        assertTrue(coordinator.contains("screenReadCoordinator.createAccessibilityPayload("))
        assertTrue(coordinator.contains("screenReadCoordinator.createOcrPayload("))
        assertTrue(coordinator.contains("screenReadCoordinator.createHybridPayload("))
        assertTrue(coordinator.contains("captureScreenshot = screenPreviewCoordinator::captureBestScreenshot"))
        assertTrue(coordinator.contains("screenPreviewCoordinator.captureBestScreenshot("))
        assertTrue(snapshotCoordinator.contains("fun getTreeSnapshotResult(): AccessibilityTreeSnapshotResult"))
        assertTrue(snapshotCoordinator.contains("fun createTreePayload("))
        assertTrue(snapshotCoordinator.contains("fun createScreenPayload("))
        assertTrue(readCoordinator.contains("captureScreenshot"))
        assertTrue(previewCoordinator.contains("fun captureBestScreenshot("))
        assertTrue(previewCoordinator.contains("fun hasMediaProjection(): Boolean"))
    }
}
