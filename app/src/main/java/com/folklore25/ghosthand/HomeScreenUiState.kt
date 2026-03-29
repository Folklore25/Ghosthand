/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

internal data class RuntimeSummaryUiState(
    val statusText: String,
    val apiStatusText: String,
    val apiTone: StatusTone,
    val serviceStatusText: String,
    val serviceTone: StatusTone,
    val accessibilityStatusText: String,
    val accessibilityTone: StatusTone,
    val actionEnabled: Boolean,
    val actionLabel: String
)

internal data class HomeCapabilitySummaryUiState(
    val detailText: String,
    val effectiveText: String,
    val effectiveTone: StatusTone
)

internal data class DiagnosticsSummaryUiState(
    val buildText: String,
    val lastActionText: String,
    val foregroundText: String
)

internal data class HomeScreenUiState(
    val versionBadgeText: String,
    val runtimeSummary: RuntimeSummaryUiState,
    val permissionsSummaryText: String,
    val accessibilitySummary: HomeCapabilitySummaryUiState,
    val screenshotSummary: HomeCapabilitySummaryUiState,
    val diagnosticsSummary: DiagnosticsSummaryUiState,
    val rootEntryLabel: String
)

internal object HomeScreenUiStateFactory {
    fun create(
        runtimeState: RuntimeState,
        textLookup: UiTextLookup = AppUiTextLookup
    ): HomeScreenUiState {
        val accessibilityUi = CapabilityUiStateFactory.forCapability(
            capability = GhosthandCapability.Accessibility,
            runtimeState = runtimeState,
            textLookup = textLookup
        )
        val screenshotUi = CapabilityUiStateFactory.forCapability(
            capability = GhosthandCapability.Screenshot,
            runtimeState = runtimeState,
            textLookup = textLookup
        )
        val policy = runtimeState.capabilityPolicy
        val access = runtimeState.capabilityAccess
        val allowedCount = listOf(
            policy.accessibilityAllowed,
            policy.screenshotAllowed,
            policy.rootAllowed
        ).count { it }
        val effectiveCount = listOf(
            access.accessibility.effectiveAvailable,
            access.screenshot.effectiveAvailable,
            access.root.effectiveAvailable
        ).count { it }

        return HomeScreenUiState(
            versionBadgeText = textLookup.getString(
                R.string.home_version_badge_template,
                localizedValue(runtimeState.buildVersion, textLookup)
            ),
            runtimeSummary = RuntimeSummaryUiState(
                statusText = runtimeState.statusText,
                apiStatusText = UiStatusSupport.booleanText(
                    textLookup,
                    runtimeState.localApiServerRunning
                ),
                apiTone = UiStatusSupport.booleanTone(runtimeState.localApiServerRunning),
                serviceStatusText = UiStatusSupport.booleanText(
                    textLookup,
                    runtimeState.foregroundServiceRunning
                ),
                serviceTone = UiStatusSupport.booleanTone(runtimeState.foregroundServiceRunning),
                accessibilityStatusText = UiStatusSupport.accessibilityStatusText(
                    textLookup,
                    runtimeState.capabilityAccess.accessibility.system.status
                ),
                accessibilityTone = UiStatusSupport.accessibilityTone(
                    runtimeState.capabilityAccess.accessibility.system.status
                ),
                actionEnabled = !runtimeState.foregroundServiceRunning,
                actionLabel = if (runtimeState.foregroundServiceRunning) {
                    textLookup.getString(R.string.service_button_running_label)
                } else {
                    textLookup.getString(R.string.service_button_label)
                }
            ),
            permissionsSummaryText = textLookup.getString(
                R.string.home_permissions_summary_template_v2,
                effectiveCount,
                3,
                allowedCount
            ),
            accessibilitySummary = HomeCapabilitySummaryUiState(
                detailText = textLookup.getString(
                    R.string.home_permission_detail_template,
                    accessibilityUi.systemLabel,
                    accessibilityUi.policyLabel
                ),
                effectiveText = accessibilityUi.effectiveLabel,
                effectiveTone = accessibilityUi.effectiveTone
            ),
            screenshotSummary = HomeCapabilitySummaryUiState(
                detailText = textLookup.getString(
                    R.string.home_permission_detail_template,
                    screenshotUi.systemLabel,
                    screenshotUi.policyLabel
                ),
                effectiveText = screenshotUi.effectiveLabel,
                effectiveTone = screenshotUi.effectiveTone
            ),
            diagnosticsSummary = DiagnosticsSummaryUiState(
                buildText = localizedValue(runtimeState.buildVersion, textLookup),
                lastActionText = if (runtimeState.lastServiceAction.isBlank()) {
                    textLookup.getString(R.string.last_service_action_default)
                } else {
                    runtimeState.lastServiceAction
                },
                foregroundText = localizedValue(runtimeState.foregroundPackage, textLookup)
            ),
            rootEntryLabel = if (runtimeState.capabilityAccess.root.system.authorized) {
                textLookup.getString(R.string.home_root_entry_available)
            } else {
                textLookup.getString(R.string.home_root_entry_default_v2)
            }
        )
    }

    private fun localizedValue(value: String?, textLookup: UiTextLookup): String {
        return if (value.isNullOrBlank() || value == "unknown") {
            textLookup.getString(R.string.runtime_placeholder_unknown)
        } else {
            value
        }
    }
}
