/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.capability.AccessibilitySystemAuthorizationState
import com.folklore25.ghosthand.capability.AppCapabilityPolicyState
import com.folklore25.ghosthand.capability.CapabilityAccessSnapshot
import com.folklore25.ghosthand.capability.CapabilityEffectiveState
import com.folklore25.ghosthand.capability.GovernedCapabilitySnapshot
import com.folklore25.ghosthand.capability.ScreenshotSystemAuthorizationState
import com.folklore25.ghosthand.state.device.PermissionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatePayloadComposerTest {
    @Test
    fun permissionsPayloadPublishesCapabilityPlanesAndCurrentBlockers() {
        val permissions = StatePayloadComposer.permissionsPayload(
            accessibilityEnabled = false,
            capabilityAccess = CapabilityAccessSnapshot(
                accessibility = GovernedCapabilitySnapshot(
                    system = AccessibilitySystemAuthorizationState(
                        enabled = false,
                        connected = false,
                        dispatchCapable = false,
                        healthy = false,
                        status = "disabled"
                    ),
                    policy = AppCapabilityPolicyState(allowed = false),
                    effective = CapabilityEffectiveState(
                        usableNow = false,
                        reason = "policy_blocked"
                    )
                ),
                screenshot = GovernedCapabilitySnapshot(
                    system = ScreenshotSystemAuthorizationState(
                        accessibilityCaptureReady = false,
                        mediaProjectionGranted = false
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(
                        usableNow = false,
                        reason = "capture_unavailable"
                    )
                )
            ),
            permissionSnapshot = PermissionSnapshot(
                usageAccess = true,
                notifications = false,
                overlay = null,
                writeSecureSettings = false
            )
        )

        val capabilitySummary = permissions["capabilitySummary"] as Map<*, *>
        val accessibility = capabilitySummary["accessibility_control"] as Map<*, *>
        val screenshot = capabilitySummary["screenshot_capture"] as Map<*, *>

        assertEquals(false, accessibility["availableNow"])
        assertEquals(false, screenshot["availableNow"])
        assertTrue((accessibility["blockers"] as List<*>).contains("policy_blocked"))
        assertTrue((screenshot["blockers"] as List<*>).contains("capture_path_unavailable"))
        assertEquals(listOf("accessibility_service"), accessibility["requiredServices"])
        assertEquals(listOf("media_projection_or_accessibility_capture"), screenshot["requiredPermissions"])
        assertTrue(accessibility.containsKey("currentBackend"))
        assertFalse(accessibility.containsKey("requiredPermissions"))
        assertFalse(screenshot.containsKey("requiredServices"))
    }
}
