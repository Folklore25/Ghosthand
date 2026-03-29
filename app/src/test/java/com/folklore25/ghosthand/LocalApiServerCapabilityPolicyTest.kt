/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalApiServerCapabilityPolicyTest {
    @Test
    fun accessibilityRouteMapIncludesClickAndWait() {
        assertEquals(GhosthandCapability.Accessibility, CapabilityRoutePolicy.routeCapability("/click"))
        assertEquals(GhosthandCapability.Accessibility, CapabilityRoutePolicy.routeCapability("/wait"))
    }

    @Test
    fun screenshotPolicyDeniedIsReturnedEvenWhenRootFallbackIsAvailable() {
        val snapshot = CapabilityAccessSnapshot(
            screenshot = GovernedCapabilitySnapshot(
                system = ScreenshotSystemAuthorizationState(
                    accessibilityCaptureReady = false,
                    mediaProjectionGranted = false,
                    rootFallbackAvailable = true
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

    @Test
    fun rootRoutesAreNotDeniedWhenPolicyIsAllowed() {
        val snapshot = CapabilityAccessSnapshot(
            root = GovernedCapabilitySnapshot(
                system = RootSystemAuthorizationState(available = true, healthy = true, status = "available"),
                policy = AppCapabilityPolicyState(allowed = true),
                effective = CapabilityEffectiveState(usableNow = true, reason = "root_available")
            )
        )

        assertNull(CapabilityRoutePolicy.policyDeniedResponse("/launch", snapshot))
    }
}
