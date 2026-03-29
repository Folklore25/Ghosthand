/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

internal class HomeScreenBinder(
    private val context: Context,
    private val versionBadge: TextView,
    private val updateInstalledValue: TextView,
    private val updateLatestValue: TextView,
    private val updateStatusValue: TextView,
    private val updateButton: Button,
    private val runtimeStatusValue: TextView,
    private val runtimeApiChip: TextView,
    private val runtimeServiceChip: TextView,
    private val runtimeAccessibilityChip: TextView,
    private val startRuntimeButton: Button,
    private val permissionSummaryValue: TextView,
    private val accessibilityRow: HomeCapabilityRowViews,
    private val screenshotRow: HomeCapabilityRowViews,
    private val diagnosticsBuildValue: TextView,
    private val diagnosticsLastActionValue: TextView,
    private val diagnosticsForegroundValue: TextView
) {
    fun bind(state: HomeScreenUiState) {
        versionBadge.text = state.versionBadgeText
        UiStatusSupport.styleChip(context, versionBadge, StatusTone.Neutral)
        updateInstalledValue.text = state.updateSummary.installedVersionText
        updateLatestValue.text = state.updateSummary.latestReleaseText
        updateStatusValue.text = state.updateSummary.statusText
        UiStatusSupport.styleChip(context, updateStatusValue, state.updateSummary.statusTone)
        if (state.updateSummary.actionLabel == null) {
            updateButton.visibility = View.GONE
        } else {
            updateButton.visibility = View.VISIBLE
            updateButton.text = state.updateSummary.actionLabel
        }
        updateButton.isEnabled = state.updateSummary.actionUrl != null

        runtimeStatusValue.text = state.runtimeSummary.statusText
        runtimeApiChip.text = state.runtimeSummary.apiStatusText
        runtimeServiceChip.text = state.runtimeSummary.serviceStatusText
        runtimeAccessibilityChip.text = state.runtimeSummary.accessibilityStatusText
        UiStatusSupport.styleChip(context, runtimeApiChip, state.runtimeSummary.apiTone)
        UiStatusSupport.styleChip(context, runtimeServiceChip, state.runtimeSummary.serviceTone)
        UiStatusSupport.styleChip(context, runtimeAccessibilityChip, state.runtimeSummary.accessibilityTone)

        startRuntimeButton.isEnabled = state.runtimeSummary.actionEnabled
        startRuntimeButton.text = state.runtimeSummary.actionLabel
        permissionSummaryValue.text = state.permissionsSummaryText

        bindCapabilityRow(accessibilityRow, state.accessibilitySummary)
        bindCapabilityRow(screenshotRow, state.screenshotSummary)

        diagnosticsBuildValue.text = state.diagnosticsSummary.buildText
        diagnosticsLastActionValue.text = state.diagnosticsSummary.lastActionText
        diagnosticsForegroundValue.text = state.diagnosticsSummary.foregroundText
    }

    private fun bindCapabilityRow(
        views: HomeCapabilityRowViews,
        uiState: HomeCapabilitySummaryUiState
    ) {
        views.summaryView.text = uiState.detailText
        views.effectiveView.text = uiState.effectiveText
        UiStatusSupport.styleChip(context, views.effectiveView, uiState.effectiveTone)
    }
}

internal data class HomeScreenViews(
    val versionBadge: TextView,
    val updateInstalledValue: TextView,
    val updateLatestValue: TextView,
    val updateStatusValue: TextView,
    val updateButton: Button,
    val runtimeStatusValue: TextView,
    val runtimeApiChip: TextView,
    val runtimeServiceChip: TextView,
    val runtimeAccessibilityChip: TextView,
    val startRuntimeButton: Button,
    val permissionSummaryValue: TextView,
    val accessibilityRow: HomeCapabilityRowViews,
    val screenshotRow: HomeCapabilityRowViews,
    val diagnosticsBuildValue: TextView,
    val diagnosticsLastActionValue: TextView,
    val diagnosticsForegroundValue: TextView,
    val managePermissionsButton: Button,
    val openDiagnosticsButton: Button,
    val updateInfoButton: android.widget.ImageButton,
    val runtimeInfoButton: android.widget.ImageButton,
    val permissionsInfoButton: android.widget.ImageButton,
    val diagnosticsInfoButton: android.widget.ImageButton
) {
    companion object {
        fun bind(activity: android.app.Activity): HomeScreenViews {
            return HomeScreenViews(
                versionBadge = activity.findViewById(R.id.homeVersionBadge),
                updateInstalledValue = activity.findViewById(R.id.homeUpdateInstalledValue),
                updateLatestValue = activity.findViewById(R.id.homeUpdateLatestValue),
                updateStatusValue = activity.findViewById(R.id.homeUpdateStatusValue),
                updateButton = activity.findViewById(R.id.homeUpdateButton),
                runtimeStatusValue = activity.findViewById(R.id.homeRuntimeStatusValue),
                runtimeApiChip = activity.findViewById(R.id.homeApiStatusValue),
                runtimeServiceChip = activity.findViewById(R.id.homeServiceStatusValue),
                runtimeAccessibilityChip = activity.findViewById(R.id.homeAccessibilityStatusValue),
                startRuntimeButton = activity.findViewById(R.id.startServiceButton),
                permissionSummaryValue = activity.findViewById(R.id.homePermissionSummaryValue),
                accessibilityRow = HomeCapabilityRowViews(
                    activity.findViewById(R.id.homeAccessibilitySummaryValue),
                    activity.findViewById(R.id.homeAccessibilityEffectiveValue)
                ),
                screenshotRow = HomeCapabilityRowViews(
                    activity.findViewById(R.id.homeScreenshotSummaryValue),
                    activity.findViewById(R.id.homeScreenshotEffectiveValue)
                ),
                diagnosticsBuildValue = activity.findViewById(R.id.homeDiagnosticsBuildValue),
                diagnosticsLastActionValue = activity.findViewById(R.id.homeDiagnosticsLastActionValue),
                diagnosticsForegroundValue = activity.findViewById(R.id.homeDiagnosticsForegroundValue),
                managePermissionsButton = activity.findViewById(R.id.openPermissionsButton),
                openDiagnosticsButton = activity.findViewById(R.id.openDiagnosticsButton),
                updateInfoButton = activity.findViewById(R.id.homeUpdateInfoButton),
                runtimeInfoButton = activity.findViewById(R.id.homeRuntimeInfoButton),
                permissionsInfoButton = activity.findViewById(R.id.homePermissionsInfoButton),
                diagnosticsInfoButton = activity.findViewById(R.id.homeDiagnosticsInfoButton)
            )
        }
    }
}

internal class HomeScreenActions(
    private val activity: AppCompatActivity,
    private val runtimeViewModel: RuntimeStateViewModel,
    private val views: HomeScreenViews
) {
    fun bind() {
        views.updateInfoButton.setOnClickListener {
            ModuleExplanationDialogFragment.show(activity.supportFragmentManager, ModuleExplanation.Update)
        }
        views.runtimeInfoButton.setOnClickListener {
            ModuleExplanationDialogFragment.show(activity.supportFragmentManager, ModuleExplanation.Runtime)
        }
        views.permissionsInfoButton.setOnClickListener {
            ModuleExplanationDialogFragment.show(activity.supportFragmentManager, ModuleExplanation.Permissions)
        }
        views.diagnosticsInfoButton.setOnClickListener {
            ModuleExplanationDialogFragment.show(activity.supportFragmentManager, ModuleExplanation.Diagnostics)
        }
        views.startRuntimeButton.setOnClickListener {
            runtimeViewModel.requestForegroundServiceStart()
            androidx.core.content.ContextCompat.startForegroundService(
                activity,
                android.content.Intent(activity, GhosthandForegroundService::class.java)
            )
            android.widget.Toast.makeText(activity, R.string.service_requested, android.widget.Toast.LENGTH_SHORT).show()
        }
        views.updateButton.setOnClickListener {
            val actionUrl = runtimeViewModel.homeScreenState.value?.updateSummary?.actionUrl
            if (actionUrl == null) {
                runtimeViewModel.refreshReleaseInfo()
                android.widget.Toast.makeText(activity, R.string.home_update_refresh_started, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(actionUrl)))
            }
        }
        views.managePermissionsButton.setOnClickListener {
            activity.startActivity(PermissionsActivity.createIntent(activity))
        }
        views.openDiagnosticsButton.setOnClickListener {
            activity.startActivity(android.content.Intent(activity, DiagnosticsActivity::class.java))
        }
    }
}
internal data class HomeCapabilityRowViews(
    val summaryView: TextView,
    val effectiveView: TextView
)
