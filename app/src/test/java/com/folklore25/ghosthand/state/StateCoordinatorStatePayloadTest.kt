/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.capability.GovernedCapabilityPayloads
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import com.folklore25.ghosthand.screen.read.ScreenReadRetryHint
import com.folklore25.ghosthand.state.StatePayloadComposer
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateCoordinatorStatePayloadTest {
    @Test
    fun statePayloadSupportSeparatesPermissionsAndSystemDiagnostics() {
        val capabilityAccess = CapabilityAccessSnapshot(
            accessibility = GovernedCapabilitySnapshot(
                system = AccessibilitySystemAuthorizationState(
                    enabled = true,
                    connected = true,
                    dispatchCapable = true,
                    healthy = true,
                    status = "enabled_connected"
                ),
                policy = AppCapabilityPolicyState(allowed = true),
                effective = CapabilityEffectiveState(
                    usableNow = true,
                    reason = "accessibility_connected"
                )
            ),
            screenshot = GovernedCapabilitySnapshot(
                system = ScreenshotSystemAuthorizationState(
                    accessibilityCaptureReady = true,
                    mediaProjectionGranted = false
                ),
                policy = AppCapabilityPolicyState(allowed = true),
                effective = CapabilityEffectiveState(
                    usableNow = true,
                    reason = "accessibility_capture_ready"
                )
            )
        )

        val permissions = StatePayloadComposer.permissionsPayload(
            accessibilityEnabled = true,
            capabilityAccess = capabilityAccess
        )
        val systemPermissions = StatePayloadComposer.systemPermissionsPayload(
            PermissionSnapshot(
                usageAccess = true,
                notifications = false,
                overlay = null,
                writeSecureSettings = false
            )
        )

        val capabilitySummary = permissions["capabilitySummary"] as Map<*, *>
        val capabilities = permissions["capabilities"] as Map<*, *>
        assertTrue(capabilitySummary.containsKey("accessibility"))
        assertTrue(capabilities.containsKey("screenshot"))
        assertEquals(true, systemPermissions["usageAccess"])
        assertEquals(false, systemPermissions["notifications"])
        assertEquals(false, systemPermissions["writeSecureSettings"])
    }

    @Test
    fun accessibilityPayloadContainsSeparateSystemPolicyAndEffectiveObjects() {
        val fields = GovernedCapabilityPayloads.accessibilityFields(
            GovernedCapabilitySnapshot(
                system = AccessibilitySystemAuthorizationState(
                    enabled = true,
                    connected = true,
                    dispatchCapable = true,
                    healthy = true,
                    status = "enabled_connected"
                ),
                policy = AppCapabilityPolicyState(allowed = true),
                effective = CapabilityEffectiveState(
                    usableNow = true,
                    reason = "accessibility_connected"
                )
            )
        )

        val system = fields["system"] as Map<*, *>
        val policy = fields["policy"] as Map<*, *>
        val effective = fields["effective"] as Map<*, *>
        assertTrue(system.containsKey("dispatchCapable"))
        assertTrue(policy.containsKey("allowed"))
        assertTrue(effective.containsKey("usableNow"))
        assertTrue(system["dispatchCapable"] as Boolean)
        assertTrue(policy["allowed"] as Boolean)
        assertTrue(effective["usableNow"] as Boolean)
    }

    @Test
    fun screenshotPayloadIncludesOnlySupportedSystemTruth() {
        val fields = GovernedCapabilityPayloads.screenshotFields(
            GovernedCapabilitySnapshot(
                system = ScreenshotSystemAuthorizationState(
                    accessibilityCaptureReady = true,
                    mediaProjectionGranted = false
                ),
                policy = AppCapabilityPolicyState(allowed = true),
                effective = CapabilityEffectiveState(
                    usableNow = true,
                    reason = "accessibility_capture_ready"
                )
            )
        )

        val system = fields["system"] as Map<*, *>
        assertTrue(system["accessibilityCaptureReady"] as Boolean)
        assertEquals(false, system["mediaProjectionGranted"] as Boolean)
    }

    @Test
    fun permissionsPayloadSeparatesGovernedCapabilitiesFromSystemPermissionDiagnostics() {
        val stateReadCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/read/StateReadCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/read/StateReadCoordinator.kt"
        )

        assertTrue(stateReadCoordinator.contains("StatePayloadComposer.createStatePayload("))
        assertFalse(stateReadCoordinator.contains("fun permissionsPayload("))
        assertFalse(stateReadCoordinator.contains("fun systemPermissionsPayload(permissionSnapshot: PermissionSnapshot): Map<String, Any?>"))
        assertFalse(stateReadCoordinator.contains(".put(\"permissions\", JSONObject()\n                .put(\"implemented\", true)\n                .put(\"usageAccess\""))
    }

    @Test
    fun capabilityPayloadsLiveInDedicatedCapabilityModule() {
        val statePayloadSupport = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StatePayloadComposer.kt",
            "src/main/java/com/folklore25/ghosthand/state/StatePayloadComposer.kt"
        )

        assertTrue(statePayloadSupport.contains("GovernedCapabilityPayloads.accessibilityToJson"))
        assertTrue(statePayloadSupport.contains("GovernedCapabilityPayloads.screenshotToJson"))
    }

    @Test
    fun postActionStateComposerOwnsCompactStateSummaryAssembly() {
        val state = PostActionStateComposer.fromObservedEffect(
            actionEffect = ActionEffectObservation(
                stateChanged = true,
                beforeSnapshotToken = "before",
                afterSnapshotToken = "after",
                finalPackageName = "com.example",
                finalActivity = "ExampleActivity"
            ),
            fallbackSnapshot = null
        )

        assertEquals("com.example", state?.packageName)
        assertEquals("ExampleActivity", state?.activity)
        assertEquals("after", state?.snapshotToken)
        assertNull(state?.renderMode)
    }

    @Test
    fun postActionStateMatchesCanonicalScreenSummaryLegibilityProjection() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "EditorActivity",
            snapshotToken = "snap",
            capturedAt = "2026-04-02T00:00:00Z",
            nodes = listOf(
                FlatAccessibilityNode(
                    nodeId = "p0@snap",
                    text = null,
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.FrameLayout",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 50,
                    centerY = 50,
                    bounds = NodeBounds(0, 0, 100, 100)
                ),
                FlatAccessibilityNode(
                    nodeId = "p0.0@snap",
                    text = "Search",
                    contentDesc = null,
                    resourceId = "search_box",
                    className = "android.widget.EditText",
                    clickable = true,
                    editable = true,
                    enabled = true,
                    focused = true,
                    scrollable = false,
                    centerX = -5,
                    centerY = 20,
                    bounds = NodeBounds(-10, 0, 0, 40)
                )
            ),
            foregroundStableDuringCapture = true
        )

        val state = PostActionStateComposer.fromObservedEffect(
            actionEffect = null,
            fallbackSnapshot = snapshot
        )
        val summary = GhosthandApiPayloads.screenSummaryFields(
            GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot,
                editableOnly = false,
                scrollableOnly = false,
                packageFilter = null,
                clickableOnly = false
            )
        )

        assertEquals(summary["focusedEditablePresent"], state?.focusedEditablePresent)
        assertEquals(summary["renderMode"], state?.renderMode)
        assertEquals(summary["surfaceReadability"], state?.surfaceReadability)
        assertEquals(summary["visualAvailable"], state?.visualAvailable)
    }

    @Test
    fun routeActionHandlersDelegatePostActionStateOwnershipToStateSummaryModule() {
        val tapClickHandlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionTapClickRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/action/ActionTapClickRouteHandlers.kt"
        )
        val motionHandlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionMotionRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/action/ActionMotionRouteHandlers.kt"
        )
        val gestureHandlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionGestureRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/action/ActionGestureRouteHandlers.kt"
        )

        assertTrue(tapClickHandlers.contains("PostActionStateComposer.fromObservedEffect("))
        assertTrue(motionHandlers.contains("PostActionStateComposer.fromObservedEffect("))
        assertTrue(gestureHandlers.contains("PostActionStateComposer.fromObservedEffect("))
        assertFalse(tapClickHandlers.contains("internal fun buildPostActionState("))
    }

    @Test
    fun coordinatorDelegatesStateReadCompositionToDedicatedStateReadCoordinator() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val stateReadCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/read/StateReadCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/read/StateReadCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val stateReadCoordinator = StateReadCoordinator("))
        assertTrue(coordinator.contains("private val stateHealthPayloads = StateHealthPayloads"))
        assertTrue(coordinator.contains("stateHealthPayloads.createHealthPayload("))
        assertTrue(coordinator.contains("stateReadCoordinator.createStatePayload()"))
        assertTrue(coordinator.contains("stateReadCoordinator.createForegroundPayload()"))
        assertTrue(coordinator.contains("stateReadCoordinator.createDevicePayload()"))
        assertTrue(coordinator.contains("stateReadCoordinator.createInfoPayload()"))
        assertFalse(coordinator.contains("stateHealthPayloads.createDevicePayload("))
        assertFalse(coordinator.contains("stateHealthPayloads.createForegroundPayload("))
        assertFalse(coordinator.contains("stateHealthPayloads.createInfoPayload("))
        assertTrue(stateReadCoordinator.contains("fun createStatePayload(): JSONObject"))
        assertTrue(stateReadCoordinator.contains("fun createForegroundPayload(): JSONObject"))
        assertTrue(stateReadCoordinator.contains("fun createDevicePayload(): JSONObject"))
        assertTrue(stateReadCoordinator.contains("fun createInfoPayload(): JSONObject"))
        assertTrue(stateReadCoordinator.contains("StatePayloadComposer"))
        assertTrue(stateReadCoordinator.contains("StateHealthPayloads"))
    }

    @Test
    fun coordinatorUsesDedicatedExecutionCollaboratorsForInputAndScreenshotAccess() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val inputPerformer = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/interaction/execution/InputOperationPerformer.kt",
            "src/main/java/com/folklore25/ghosthand/interaction/execution/InputOperationPerformer.kt"
        )
        val screenshotAccess = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt",
            "src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt"
        )
        val previewCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val inputOperationPerformer = InputOperationPerformer"))
        assertTrue(coordinator.contains("private val screenshotAccess: GhosthandScreenshotAccess = AccessibilityScreenshotAccess"))
        assertTrue(coordinator.contains("inputOperationPerformer.perform("))
        assertTrue(coordinator.contains("private val screenPreviewCoordinator = ScreenPreviewCoordinator("))
        assertTrue(coordinator.contains("screenPreviewCoordinator.captureBestScreenshot("))
        assertTrue(inputPerformer.contains("fun perform("))
        assertTrue(screenshotAccess.contains("fun captureBestAvailable("))
        assertTrue(previewCoordinator.contains("fun captureBestScreenshot("))
    }
}
