/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.main

import com.folklore25.ghosthand.ui.diagnostics.DiagnosticsActivity
import com.folklore25.ghosthand.service.runtime.GhosthandForegroundService
import com.folklore25.ghosthand.ui.common.model.ModuleExplanation
import com.folklore25.ghosthand.ui.common.dialog.ModuleExplanationDialogFragment
import com.folklore25.ghosthand.ui.permissions.PermissionsActivity
import com.folklore25.ghosthand.state.runtime.RuntimeStateStore
import com.folklore25.ghosthand.ui.common.model.UiStatusSupport
import com.folklore25.ghosthand.ui.common.dialog.UpdateDialogFragment

import com.folklore25.ghosthand.R

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

internal class HomeScreenBinder(
    private val context: Context,
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
    private val diagnosticsLastActionValue: TextView
) {
    fun bind(state: HomeScreenUiState) {
        updateButton.visibility = View.VISIBLE
        updateButton.isEnabled = true

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
    val managePermissionsButton: Button,
    val openDiagnosticsButton: Button,
    val runtimeInfoButton: TextView,
    val permissionsInfoButton: TextView,
    val diagnosticsInfoButton: TextView
) {
    companion object {
        fun bind(activity: android.app.Activity): HomeScreenViews {
            return HomeScreenViews(
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
                managePermissionsButton = activity.findViewById(R.id.openPermissionsButton),
                openDiagnosticsButton = activity.findViewById(R.id.openDiagnosticsButton),
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
            try {
                ContextCompat.startForegroundService(
                    activity,
                    android.content.Intent(activity, GhosthandForegroundService::class.java)
                )
                android.widget.Toast.makeText(activity, R.string.service_requested, android.widget.Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                val state = RuntimeStateStore.snapshot()
                RuntimeStateStore.recordRuntimeStartFailure(
                    preconditions = "serviceRunning=${state.foregroundServiceRunning} source=home_button",
                    error = error
                )
                Log.e(
                    LOG_TAG,
                    "event=start_runtime_failed source=home_button serviceRunning=${state.foregroundServiceRunning} failureClass=${error.javaClass.simpleName} fallback=runtime_remains_stopped",
                    error
                )
                android.widget.Toast.makeText(activity, R.string.runtime_start_failed_toast, android.widget.Toast.LENGTH_LONG).show()
            }
        }
        views.updateButton.setOnClickListener {
            UpdateDialogFragment.show(activity.supportFragmentManager)
        }
        views.managePermissionsButton.setOnClickListener {
            activity.startActivity(PermissionsActivity.createIntent(activity))
        }
        views.openDiagnosticsButton.setOnClickListener {
            activity.startActivity(android.content.Intent(activity, DiagnosticsActivity::class.java))
        }
    }

    private companion object {
        const val LOG_TAG = "GhostBootstrap"
    }
}
internal data class HomeCapabilityRowViews(
    val summaryView: TextView,
    val effectiveView: TextView
)
