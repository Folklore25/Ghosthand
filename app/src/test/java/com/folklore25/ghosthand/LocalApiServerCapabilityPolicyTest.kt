/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import com.folklore25.ghosthand.server.CapabilityRoutePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalApiServerCapabilityPolicyTest {
    @Test
    fun accessibilityRouteMapIncludesClickAndWaitOnly() {
        assertEquals(GhosthandCapability.Accessibility, CapabilityRoutePolicy.routeCapability("/click"))
        assertEquals(GhosthandCapability.Accessibility, CapabilityRoutePolicy.routeCapability("/wait"))
        assertNull(CapabilityRoutePolicy.routeCapability("/launch"))
        assertNull(CapabilityRoutePolicy.routeCapability("/stop"))
    }

    @Test
    fun screenshotPolicyDeniedIsReturnedWhenPolicyIsOff() {
        val snapshot = CapabilityAccessSnapshot(
            screenshot = GovernedCapabilitySnapshot(
                system = ScreenshotSystemAuthorizationState(
                    accessibilityCaptureReady = false,
                    mediaProjectionGranted = true
                ),
                policy = AppCapabilityPolicyState(allowed = false),
                effective = CapabilityEffectiveState(
                    usableNow = false,
                    reason = "policy_blocked"
                )
            )
        )

        assertEquals(
            CapabilityRoutePolicy.denialMessage(GhosthandCapability.Screenshot),
            CapabilityRoutePolicy.policyDeniedResponse("/screenshot", snapshot)
        )
    }
}
