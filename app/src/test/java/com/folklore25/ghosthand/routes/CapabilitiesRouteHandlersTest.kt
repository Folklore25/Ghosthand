/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.AccessibilitySystemAuthorizationState
import com.folklore25.ghosthand.AppCapabilityPolicyState
import com.folklore25.ghosthand.CapabilityAccessSnapshot
import com.folklore25.ghosthand.CapabilityEffectiveState
import com.folklore25.ghosthand.GovernedCapabilitySnapshot
import com.folklore25.ghosthand.PermissionSnapshot
import com.folklore25.ghosthand.ScreenshotSystemAuthorizationState
import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesRouteHandlersTest {
    @Test
    fun capabilitiesPayloadIsCapabilityCentric() {
        val payload = GhosthandCapabilityPresentation.capabilitiesFields(
            capabilityAccess = CapabilityAccessSnapshot(
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
            ),
            permissionSnapshot = PermissionSnapshot(
                usageAccess = true,
                notifications = true,
                overlay = null,
                writeSecureSettings = false
            )
        )

        assertEquals("1.0", payload["schemaVersion"])
        val capabilities = payload["capabilities"] as List<*>
        val accessibilityControl = capabilities
            .map { it as Map<*, *> }
            .first { it["capabilityId"] == "accessibility_control" }
        val availability = accessibilityControl["availability"] as Map<*, *>

        assertEquals("control", accessibilityControl["domain"])
        assertEquals("primitive", accessibilityControl["kind"])
        assertEquals("action_truth", accessibilityControl["truthType"])
        assertTrue((accessibilityControl["routes"] as List<*>).contains("/click"))
        assertEquals(true, availability["availableNow"])
        assertTrue((availability["requiredServices"] as List<*>).contains("accessibility_service"))
    }
}
