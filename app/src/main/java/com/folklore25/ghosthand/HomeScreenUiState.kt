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

internal data class UpdateSummaryUiState(
    val titleText: String,
    val subtitleText: String,
    val installedVersionText: String,
    val latestReleaseText: String,
    val statusText: String,
    val statusTone: StatusTone,
    val actionLabel: String?,
    val actionUrl: String?
)

internal data class HomeScreenUiState(
    val versionBadgeText: String,
    val updateSummary: UpdateSummaryUiState,
    val runtimeSummary: RuntimeSummaryUiState,
    val permissionsSummaryText: String,
    val accessibilitySummary: HomeCapabilitySummaryUiState,
    val screenshotSummary: HomeCapabilitySummaryUiState,
    val diagnosticsSummary: DiagnosticsSummaryUiState
)

internal object HomeScreenUiStateFactory {
    fun create(
        runtimeState: RuntimeState,
        updateUiState: UpdateUiState,
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
            policy.screenshotAllowed
        ).count { it }
        val effectiveCount = listOf(
            access.accessibility.effectiveAvailable,
            access.screenshot.effectiveAvailable
        ).count { it }

        return HomeScreenUiState(
            versionBadgeText = textLookup.getString(
                R.string.home_version_badge_template,
                localizedValue(runtimeState.buildVersion, textLookup)
            ),
            updateSummary = updateSummary(updateUiState, textLookup),
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
                2,
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
            )
        )
    }

    private fun localizedValue(value: String?, textLookup: UiTextLookup): String {
        return if (value.isNullOrBlank() || value == "unknown") {
            textLookup.getString(R.string.runtime_placeholder_unknown)
        } else {
            value
        }
    }

    private fun updateSummary(
        updateUiState: UpdateUiState,
        textLookup: UiTextLookup
    ): UpdateSummaryUiState {
        val installedVersion = if (updateUiState.installedVersionText.isBlank()) {
            textLookup.getString(R.string.runtime_placeholder_unknown)
        } else {
            updateUiState.installedVersionText
        }
        val latestRelease = updateUiState.latestReleaseText
            ?: textLookup.getString(R.string.home_update_latest_unknown)

        val statusText = when (updateUiState.status) {
            UpdateStatus.CHECKING -> textLookup.getString(R.string.home_update_status_checking)
            UpdateStatus.UP_TO_DATE -> textLookup.getString(R.string.home_update_status_up_to_date)
            UpdateStatus.UPDATE_AVAILABLE -> textLookup.getString(R.string.home_update_status_available)
            UpdateStatus.CHECK_FAILED -> updateUiState.failureReason
                ?: textLookup.getString(R.string.home_update_status_failed)
        }

        val statusTone = when (updateUiState.status) {
            UpdateStatus.CHECKING -> StatusTone.Neutral
            UpdateStatus.UP_TO_DATE -> StatusTone.Success
            UpdateStatus.UPDATE_AVAILABLE -> StatusTone.Warning
            UpdateStatus.CHECK_FAILED -> StatusTone.Danger
        }

        val actionLabel = if (updateUiState.status == UpdateStatus.UPDATE_AVAILABLE &&
            !updateUiState.actionUrl.isNullOrBlank()
        ) {
            textLookup.getString(R.string.home_update_action_download)
        } else {
            null
        }

        return UpdateSummaryUiState(
            titleText = textLookup.getString(R.string.home_update_title),
            subtitleText = textLookup.getString(R.string.home_update_subtitle),
            installedVersionText = textLookup.getString(
                R.string.home_update_installed_template,
                installedVersion
            ),
            latestReleaseText = textLookup.getString(
                R.string.home_update_latest_template,
                latestRelease
            ),
            statusText = statusText,
            statusTone = statusTone,
            actionLabel = actionLabel,
            actionUrl = updateUiState.actionUrl
        )
    }
}
