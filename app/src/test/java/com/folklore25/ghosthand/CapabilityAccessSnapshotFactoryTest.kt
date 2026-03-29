/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityAccessSnapshotFactoryTest {
    @Test
    fun accessibilityRequiresPolicyAndConnectedService() {
        val snapshot = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = AccessibilityStatusSnapshot(
                implemented = true,
                enabled = true,
                connected = true,
                dispatchCapable = true,
                healthy = true,
                status = "enabled_connected"
            ),
            mediaProjectionGranted = false,
            rootAvailability = RootAvailabilitySnapshot(
                implemented = true,
                available = false,
                healthy = false,
                status = "unavailable"
            ),
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false,
                rootAllowed = false
            )
        )

        assertTrue(snapshot.accessibility.system.enabled)
        assertTrue(snapshot.accessibility.system.connected)
        assertTrue(snapshot.accessibility.effective.usableNow)
        assertEquals("accessibility_connected", snapshot.accessibility.effective.reason)
    }

    @Test
    fun accessibilityEnabledButIdleIsNotYetUsable() {
        val snapshot = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = AccessibilityStatusSnapshot(
                implemented = true,
                enabled = true,
                connected = true,
                dispatchCapable = false,
                healthy = null,
                status = "enabled_idle"
            ),
            mediaProjectionGranted = false,
            rootAvailability = RootAvailabilitySnapshot(
                implemented = true,
                available = false,
                healthy = false,
                status = "unavailable"
            ),
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false,
                rootAllowed = false
            )
        )

        assertTrue(snapshot.accessibility.system.enabled)
        assertFalse(snapshot.accessibility.system.dispatchCapable)
        assertFalse(snapshot.accessibility.effective.usableNow)
        assertEquals("service_idle", snapshot.accessibility.effective.reason)
    }

    @Test
    fun screenshotCanUseRootFallbackOnlyWhenBothPoliciesAllowIt() {
        val snapshot = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = AccessibilityStatusSnapshot(
                implemented = true,
                enabled = false,
                connected = false,
                dispatchCapable = false,
                healthy = false,
                status = "disabled"
            ),
            mediaProjectionGranted = false,
            rootAvailability = RootAvailabilitySnapshot(
                implemented = true,
                available = true,
                healthy = true,
                status = "available"
            ),
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = false,
                screenshotAllowed = true,
                rootAllowed = true
            )
        )

        assertTrue(snapshot.root.effective.usableNow)
        assertTrue(snapshot.screenshot.system.rootFallbackAvailable)
        assertTrue(snapshot.screenshot.effective.usableNow)
        assertEquals("root_fallback", snapshot.screenshot.effective.reason)
    }

    @Test
    fun screenshotRemainsBlockedWhenPolicyIsOffEvenIfSystemPathExists() {
        val snapshot = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = AccessibilityStatusSnapshot(
                implemented = true,
                enabled = true,
                connected = true,
                dispatchCapable = true,
                healthy = true,
                status = "enabled_connected"
            ),
            mediaProjectionGranted = true,
            rootAvailability = RootAvailabilitySnapshot(
                implemented = true,
                available = true,
                healthy = true,
                status = "available"
            ),
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false,
                rootAllowed = true
            )
        )

        assertTrue(snapshot.screenshot.system.mediaProjectionGranted)
        assertTrue(snapshot.screenshot.system.authorized)
        assertFalse(snapshot.screenshot.effective.usableNow)
        assertEquals("policy_blocked", snapshot.screenshot.effective.reason)
    }

    @Test
    fun rootAuthorizationRequiredStaysNonEffectiveEvenWhenPolicyIsEnabled() {
        val snapshot = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = AccessibilityStatusSnapshot(
                implemented = true,
                enabled = false,
                connected = false,
                dispatchCapable = false,
                healthy = false,
                status = "disabled"
            ),
            mediaProjectionGranted = false,
            rootAvailability = RootAvailabilitySnapshot(
                implemented = true,
                available = false,
                healthy = null,
                status = "authorization_required"
            ),
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = false,
                screenshotAllowed = false,
                rootAllowed = true
            )
        )

        assertEquals("authorization_required", snapshot.root.system.status)
        assertFalse(snapshot.root.effective.usableNow)
        assertEquals("authorization_required", snapshot.root.effective.reason)
    }
}
