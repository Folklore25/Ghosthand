/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.main

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.AccessibilitySystemAuthorizationState
import com.folklore25.ghosthand.capability.AppCapabilityPolicyState
import com.folklore25.ghosthand.capability.CapabilityAccessSnapshot
import com.folklore25.ghosthand.capability.CapabilityEffectiveState
import com.folklore25.ghosthand.capability.CapabilityPolicySnapshot
import com.folklore25.ghosthand.capability.GovernedCapabilitySnapshot
import com.folklore25.ghosthand.capability.ScreenshotSystemAuthorizationState
import com.folklore25.ghosthand.integration.github.GitHubReleaseCheckResult
import com.folklore25.ghosthand.integration.github.GitHubReleaseInfo
import com.folklore25.ghosthand.integration.github.InstalledAppVersion
import com.folklore25.ghosthand.state.runtime.RuntimeState
import com.folklore25.ghosthand.ui.diagnostics.UpdateUiStateFactory
import com.folklore25.ghosthand.ui.permissions.PermissionsScreenUiStateFactory
import com.folklore25.ghosthand.ui.permissions.UiTextLookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenUiStateMapperTest {
    @Test
    fun homeScreenStateDerivesPermissionSummaryWithoutRootEntry() {
        val state = sampleRuntimeState()
        val updateState = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.UpdateAvailable(
                installedVersion = InstalledAppVersion("1.0.0", 1),
                latestRelease = GitHubReleaseInfo(
                    tagName = "v1.1.0",
                    name = "1.1.0",
                    htmlUrl = "https://github.com/folklore25/ghosthand/releases/tag/v1.1.0",
                    publishedAt = "2026-03-30T00:00:00Z",
                    apkAssetUrl = null
                )
            )
        )

        val uiState = HomeScreenUiStateFactory.create(state, updateState, FakeTextLookup)

        assertTrue(uiState.permissionsSummaryText.contains("usable=1/2 allowed=1"))
        assertEquals("System:Enabled, connected Policy:Allowed", uiState.accessibilitySummary.detailText)
        assertFalse(uiState.runtimeSummary.actionEnabled)
        assertEquals("Installed: 1.0.0", uiState.updateSummary.installedVersionText)
        assertEquals("Latest: 1.1.0", uiState.updateSummary.latestReleaseText)
        assertEquals("Update available", uiState.updateSummary.statusText)
        assertEquals("Update", uiState.updateSummary.actionLabel)
        assertTrue(uiState.updateSummary.actionEnabled)
        assertEquals(UpdateActionMode.OPEN_RELEASE, uiState.updateSummary.actionMode)
    }

    @Test
    fun upToDateStateStillOffersManualRecheck() {
        val uiState = HomeScreenUiStateFactory.create(
            sampleRuntimeState(),
            UpdateUiStateFactory.fromReleaseCheck(
                GitHubReleaseCheckResult.UpToDate(
                    installedVersion = InstalledAppVersion("1.1.0", 2),
                    latestRelease = GitHubReleaseInfo(
                        tagName = "v1.1.0",
                        name = "1.1.0",
                        htmlUrl = "https://github.com/folklore25/ghosthand/releases/tag/v1.1.0",
                        publishedAt = "2026-03-30T00:00:00Z",
                        apkAssetUrl = null
                    )
                )
            ),
            FakeTextLookup
        )

        assertEquals("Up to date", uiState.updateSummary.statusText)
        assertEquals("Check again", uiState.updateSummary.actionLabel)
        assertTrue(uiState.updateSummary.actionEnabled)
        assertEquals(UpdateActionMode.REFRESH, uiState.updateSummary.actionMode)
    }

    @Test
    fun failedCheckStillOffersRetryAndShowsInstalledVersion() {
        val uiState = HomeScreenUiStateFactory.create(
            sampleRuntimeState(),
            UpdateUiStateFactory.fromReleaseCheck(
                GitHubReleaseCheckResult.Failed(
                    installedVersion = InstalledAppVersion("1.0.0", 1),
                    reason = "Unable to read latest release metadata."
                )
            ),
            FakeTextLookup
        )

        assertEquals("Unable to read latest release metadata.", uiState.updateSummary.statusText)
        assertEquals("Installed: 1.0.0", uiState.updateSummary.installedVersionText)
        assertEquals("Latest: Not available yet", uiState.updateSummary.latestReleaseText)
        assertEquals("Check again", uiState.updateSummary.actionLabel)
        assertTrue(uiState.updateSummary.actionEnabled)
        assertEquals(UpdateActionMode.REFRESH, uiState.updateSummary.actionMode)
    }

    @Test
    fun checkingStateShowsDisabledRefreshWhileKeepingInstalledVersion() {
        val uiState = HomeScreenUiStateFactory.create(
            sampleRuntimeState(),
            UpdateUiStateFactory.fromReleaseCheck(
                GitHubReleaseCheckResult.Checking(
                    installedVersion = InstalledAppVersion("1.0.0", 1)
                )
            ),
            FakeTextLookup
        )

        assertEquals("Installed: 1.0.0", uiState.updateSummary.installedVersionText)
        assertEquals("Checking GitHub release", uiState.updateSummary.statusText)
        assertEquals("Checking", uiState.updateSummary.actionLabel)
        assertFalse(uiState.updateSummary.actionEnabled)
        assertEquals(UpdateActionMode.REFRESH, uiState.updateSummary.actionMode)
    }

    @Test
    fun permissionsScreenStateKeepsSystemPolicyAndEffectiveSeparate() {
        val state = sampleRuntimeState()

        val uiState = PermissionsScreenUiStateFactory.create(state, FakeTextLookup)

        assertEquals("Enabled, connected", uiState.accessibility.systemLabel)
        assertEquals("Allowed", uiState.accessibility.policyLabel)
        assertEquals("Usable now", uiState.accessibility.effectiveLabel)
        assertEquals("Missing", uiState.screenshot.systemLabel)
        assertEquals("Blocked", uiState.screenshot.policyLabel)
        assertEquals("Blocked by policy", uiState.screenshot.effectiveLabel)
    }

    @Test
    fun authorizeLabelsAreDerivedCentrally() {
        val uiState = PermissionsScreenUiStateFactory.create(sampleRuntimeState(), FakeTextLookup)

        assertEquals("Review Authorization", uiState.accessibility.authorizeLabel)
        assertEquals("Grant Screenshot Consent", uiState.screenshot.authorizeLabel)
    }

    private fun sampleRuntimeState(): RuntimeState {
        return RuntimeState(
            buildVersion = "1.0 (1)",
            localApiServerRunning = true,
            foregroundServiceRunning = true,
            statusText = "Runtime live",
            capabilityPolicy = CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = false
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
                        mediaProjectionGranted = false
                    ),
                    policy = AppCapabilityPolicyState(allowed = false),
                    effective = CapabilityEffectiveState(usableNow = false, reason = "policy_blocked")
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
                R.string.permission_policy_allowed -> "Allowed"
                R.string.permission_policy_blocked -> "Blocked"
                R.string.permission_screenshot_system_projection -> "MediaProjection granted"
                R.string.permission_screenshot_system_accessibility -> "Accessibility capture ready"
                R.string.permission_system_missing -> "Missing"
                R.string.permission_effective_available -> "Usable now"
                R.string.permission_effective_unavailable -> "Not usable"
                R.string.permission_effective_policy_blocked -> "Blocked by policy"
                R.string.permission_effective_authorization_required -> "Authorization required"
                R.string.permission_effective_service_idle -> "Waiting for service"
                R.string.permission_review_button -> "Review Authorization"
                R.string.permission_authorize_accessibility_button -> "Open Accessibility Settings"
                R.string.permission_authorize_screenshot_button -> "Grant Screenshot Consent"
                R.string.home_version_badge_template -> "v${args[0]}"
                R.string.home_update_title -> "Updates"
                R.string.home_update_subtitle -> "Release handoff"
                R.string.home_update_installed_template -> "Installed: ${args[0]}"
                R.string.home_update_latest_template -> "Latest: ${args[0]}"
                R.string.home_update_latest_unknown -> "Not available yet"
                R.string.home_update_status_checking -> "Checking GitHub release"
                R.string.home_update_status_up_to_date -> "Up to date"
                R.string.home_update_status_available -> "Update available"
                R.string.home_update_status_failed -> "Update check unavailable"
                R.string.home_update_action_checking -> "Checking"
                R.string.home_update_action_download -> "Update"
                R.string.home_update_action_refresh -> "Check again"
                R.string.service_button_running_label -> "Runtime Active"
                R.string.service_button_label -> "Start Runtime"
                R.string.home_permissions_summary_template -> "usable=${args[0]}/${args[1]} allowed=${args[2]}"
                R.string.home_permission_detail_template -> "System:${args[0]} Policy:${args[1]}"
                R.string.last_service_action_default -> "Not started"
                R.string.runtime_placeholder_unknown -> "Unknown"
                else -> "res-$resId"
            }
        }
    }
}
