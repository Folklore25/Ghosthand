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
    fun accessibilityRequiresPolicyAndDispatchCapableService() {
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
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false
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
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false
            )
        )

        assertTrue(snapshot.accessibility.system.enabled)
        assertFalse(snapshot.accessibility.system.dispatchCapable)
        assertFalse(snapshot.accessibility.effective.usableNow)
        assertEquals("service_idle", snapshot.accessibility.effective.reason)
    }

    @Test
    fun screenshotUsesOnlyAccessibilityAndMediaProjectionTruth() {
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
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = false,
                screenshotAllowed = true
            )
        )

        assertFalse(snapshot.screenshot.system.authorized)
        assertFalse(snapshot.screenshot.effective.usableNow)
        assertEquals("system_missing", snapshot.screenshot.effective.reason)
    }

    @Test
    fun screenshotRemainsBlockedWhenPolicyIsOffEvenIfMediaProjectionExists() {
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
            policy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false
            )
        )

        assertTrue(snapshot.screenshot.system.mediaProjectionGranted)
        assertTrue(snapshot.screenshot.system.authorized)
        assertFalse(snapshot.screenshot.effective.usableNow)
        assertEquals("policy_blocked", snapshot.screenshot.effective.reason)
    }
}
