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

internal data class HomeCapabilityRowViews(
    val summaryView: TextView,
    val effectiveView: TextView
)
