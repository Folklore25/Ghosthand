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

class ScreenUiStateMapperTest {
    @Test
    fun homeScreenStateDerivesPermissionSummaryAndBottomRootEntry() {
        val state = sampleRuntimeState()

        val uiState = HomeScreenUiStateFactory.create(state, FakeTextLookup)

        assertTrue(uiState.permissionsSummaryText.contains("usable=1/3 allowed=2"))
        assertEquals("System:Enabled, connected Policy:Allowed", uiState.accessibilitySummary.detailText)
        assertEquals("Advanced Root Entry", uiState.rootEntryLabel)
        assertFalse(uiState.runtimeSummary.actionEnabled)
    }

    @Test
    fun permissionsScreenStateKeepsSystemPolicyAndEffectiveSeparate() {
        val state = sampleRuntimeState()

        val uiState = PermissionsScreenUiStateFactory.create(state, FakeTextLookup)

        assertEquals("Enabled, connected", uiState.accessibility.systemLabel)
        assertEquals("Allowed", uiState.accessibility.policyLabel)
        assertEquals("Usable now", uiState.accessibility.effectiveLabel)
        assertEquals("Root fallback available", uiState.screenshot.systemLabel)
        assertEquals("Blocked", uiState.screenshot.policyLabel)
        assertEquals("Blocked by policy", uiState.screenshot.effectiveLabel)
    }

    @Test
    fun authorizeLabelsAreDerivedCentrally() {
        val uiState = PermissionsScreenUiStateFactory.create(sampleRuntimeState(), FakeTextLookup)

        assertEquals("Review Authorization", uiState.accessibility.authorizeLabel)
        assertEquals("Grant Screenshot Consent", uiState.screenshot.authorizeLabel)
        assertEquals("Check Root Authorization", uiState.root.authorizeLabel)
    }

    private fun sampleRuntimeState(): RuntimeState {
        return RuntimeState(
            buildVersion = "1.0 (1)",
            localApiServerRunning = true,
            foregroundServiceRunning = true,
            statusText = "Runtime live",
            capabilityPolicy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false,
                rootAllowed = true
            ),
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
                    effective = CapabilityEffectiveState(usableNow = true, reason = "accessibility_connected")
                ),
                screenshot = GovernedCapabilitySnapshot(
                    system = ScreenshotSystemAuthorizationState(
                        accessibilityCaptureReady = false,
                        mediaProjectionGranted = false,
                        rootFallbackAvailable = true
                    ),
                    policy = AppCapabilityPolicyState(allowed = false),
                    effective = CapabilityEffectiveState(usableNow = false, reason = "policy_blocked")
                ),
                root = GovernedCapabilitySnapshot(
                    system = RootSystemAuthorizationState(
                        available = false,
                        healthy = null,
                        status = "authorization_required"
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(usableNow = false, reason = "authorization_required")
                )
            ),
            lastServiceAction = "Foreground service running",
            foregroundPackage = "com.reddit.frontpage",
            accessibilityStatus = "enabled_connected"
        )
    }

    private object FakeTextLookup : UiTextLookup {
        override fun getString(resId: Int, vararg args: Any): String {
            return when (resId) {
                R.string.runtime_boolean_true -> "Yes"
                R.string.runtime_boolean_false -> "No"
                R.string.accessibility_status_disabled -> "Disabled"
                R.string.accessibility_status_enabled_idle -> "Enabled, idle"
                R.string.accessibility_status_enabled_connected -> "Enabled, connected"
                R.string.root_status_available -> "Available"
                R.string.root_status_authorization_required -> "Authorization required"
                R.string.root_status_unavailable -> "Unavailable"
                R.string.permission_policy_allowed -> "Allowed"
                R.string.permission_policy_blocked -> "Blocked"
                R.string.permission_screenshot_system_projection -> "MediaProjection granted"
                R.string.permission_screenshot_system_accessibility -> "Accessibility capture ready"
                R.string.permission_screenshot_system_root_fallback -> "Root fallback available"
                R.string.permission_system_missing -> "Missing"
                R.string.permission_effective_available -> "Usable now"
                R.string.permission_effective_root_fallback -> "Usable via root"
                R.string.permission_effective_unavailable -> "Not usable"
                R.string.permission_effective_policy_blocked -> "Blocked by policy"
                R.string.permission_effective_authorization_required -> "Authorization required"
                R.string.permission_effective_service_idle -> "Waiting for service"
                R.string.permission_review_button -> "Review Authorization"
                R.string.permission_authorize_accessibility_button -> "Open Accessibility Settings"
                R.string.permission_authorize_screenshot_button -> "Grant Screenshot Consent"
                R.string.permission_authorize_root_button -> "Check Root Authorization"
                R.string.permission_refresh_root_button -> "Refresh Root Authorization"
                R.string.home_version_badge_template -> "v${args[0]}"
                R.string.service_button_running_label -> "Runtime Active"
                R.string.service_button_label -> "Start Runtime"
                R.string.home_permissions_summary_template_v2 -> "usable=${args[0]}/${args[1]} allowed=${args[2]}"
                R.string.home_permission_detail_template -> "System:${args[0]} Policy:${args[1]}"
                R.string.last_service_action_default -> "Not started"
                R.string.runtime_placeholder_unknown -> "Unknown"
                R.string.home_root_entry_available -> "Root Permissions Available"
                R.string.home_root_entry_default_v2 -> "Advanced Root Entry"
                else -> "res-$resId"
            }
        }
    }
}
