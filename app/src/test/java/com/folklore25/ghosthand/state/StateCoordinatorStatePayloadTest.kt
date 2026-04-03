/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import com.folklore25.ghosthand.capability.GovernedCapabilityPayloads
import com.folklore25.ghosthand.state.StatePayloadComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            "app/src/main/java/com/folklore25/ghosthand/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/StateCoordinator.kt"
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
}
