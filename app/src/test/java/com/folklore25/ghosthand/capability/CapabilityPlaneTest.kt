/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

import com.folklore25.ghosthand.AccessibilitySystemAuthorizationState
import com.folklore25.ghosthand.AppCapabilityPolicyState
import com.folklore25.ghosthand.CapabilityAccessSnapshot
import com.folklore25.ghosthand.CapabilityEffectiveState
import com.folklore25.ghosthand.GovernedCapabilitySnapshot
import com.folklore25.ghosthand.PermissionSnapshot
import com.folklore25.ghosthand.ScreenshotSystemAuthorizationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityPlaneTest {
    @Test
    fun definitionsExposeCanonicalCapabilitySurface() {
        val definitions = GhosthandCapabilityDefinitions.definitions
        val capabilityIds = definitions.map { it.capabilityId }

        assertTrue(capabilityIds.contains("accessibility_control"))
        assertTrue(capabilityIds.contains("accessibility_observation"))
        assertTrue(capabilityIds.contains("screenshot_capture"))
        assertTrue(capabilityIds.contains("event_observation"))
        assertTrue(capabilityIds.contains("route_contract_catalog"))
        assertEquals(
            listOf("/tap", "/click", "/input", "/setText", "/scroll", "/swipe", "/longpress", "/gesture", "/back", "/home", "/recents"),
            GhosthandCapabilityDefinitions.definition("accessibility_control").routes
        )
    }

    @Test
    fun availabilityIsDerivedFromRuntimeStateInsteadOfStaticDuplication() {
        val availability = GhosthandCapabilityAvailabilityResolver.resolveAll(
            capabilityAccess = CapabilityAccessSnapshot(
                accessibility = GovernedCapabilitySnapshot(
                    system = AccessibilitySystemAuthorizationState(
                        enabled = true,
                        connected = false,
                        dispatchCapable = false,
                        healthy = null,
                        status = "enabled_idle"
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(
                        usableNow = false,
                        reason = "accessibility_disconnected"
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
                        reason = "capture_path_unavailable"
                    )
                )
            ),
            permissionSnapshot = PermissionSnapshot(
                usageAccess = true,
                notifications = false,
                overlay = null,
                writeSecureSettings = false
            )
        ).associateBy { it.capabilityId }

        assertEquals(false, availability.getValue("accessibility_control").availableNow)
        assertTrue(availability.getValue("accessibility_control").blockers.contains("service_disconnected"))
        assertEquals(false, availability.getValue("screenshot_capture").availableNow)
        assertTrue(availability.getValue("screenshot_capture").blockers.contains("capture_path_unavailable"))
        assertEquals(true, availability.getValue("event_observation").availableNow)
        assertEquals(true, availability.getValue("notification_access").degraded)
    }
}
