/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.capability.GovernedCapabilityPayloads
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
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )

        assertTrue(coordinator.contains("private val statePayloadComposer = StatePayloadComposer"))
        assertTrue(coordinator.contains("statePayloadComposer.createStatePayload("))
        assertFalse(coordinator.contains("fun permissionsPayload("))
        assertFalse(coordinator.contains("fun systemPermissionsPayload(permissionSnapshot: PermissionSnapshot): Map<String, Any?>"))
        assertFalse(coordinator.contains(".put(\"permissions\", JSONObject()\n                .put(\"implemented\", true)\n                .put(\"usageAccess\""))
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
    fun coordinatorDelegatesRuntimePayloadAssemblyToStateHealthModule() {
        val coordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val healthPayloads = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/health/StateHealthPayloads.kt",
            "src/main/java/com/folklore25/ghosthand/state/health/StateHealthPayloads.kt"
        )

        assertTrue(coordinator.contains("private val stateHealthPayloads = StateHealthPayloads"))
        assertTrue(coordinator.contains("stateHealthPayloads.createHealthPayload("))
        assertTrue(coordinator.contains("stateHealthPayloads.createDevicePayload("))
        assertTrue(coordinator.contains("stateHealthPayloads.createForegroundPayload("))
        assertTrue(coordinator.contains("stateHealthPayloads.createInfoPayload("))
        assertFalse(coordinator.contains("fun createHealthPayload(): JSONObject {\n        val runtimeState = runtimeStateProvider()"))
        assertTrue(healthPayloads.contains("fun createHealthPayload("))
        assertTrue(healthPayloads.contains("fun createInfoPayload("))
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

        assertTrue(coordinator.contains("private val inputOperationPerformer = InputOperationPerformer"))
        assertTrue(coordinator.contains("private val screenshotAccess: GhosthandScreenshotAccess = AccessibilityScreenshotAccess"))
        assertTrue(coordinator.contains("inputOperationPerformer.perform("))
        assertTrue(coordinator.contains("screenshotAccess.captureBestAvailable("))
        assertTrue(inputPerformer.contains("fun perform("))
        assertTrue(screenshotAccess.contains("fun captureBestAvailable("))
    }
}
