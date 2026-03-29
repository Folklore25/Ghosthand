/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateCoordinatorStatePayloadTest {
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
}
