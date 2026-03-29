/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]

        val versionBadge: TextView = findViewById(R.id.homeVersionBadge)
        val updateButton: Button = findViewById(R.id.homeUpdateButton)
        val githubButton: Button = findViewById(R.id.homeGithubButton)
        val runtimeStatusValue: TextView = findViewById(R.id.homeRuntimeStatusValue)
        val runtimeApiChip: TextView = findViewById(R.id.homeApiStatusValue)
        val runtimeServiceChip: TextView = findViewById(R.id.homeServiceStatusValue)
        val runtimeAccessibilityChip: TextView = findViewById(R.id.homeAccessibilityStatusValue)
        val startRuntimeButton: Button = findViewById(R.id.startServiceButton)

        val permissionSummaryValue: TextView = findViewById(R.id.homePermissionSummaryValue)
        val accessibilitySummaryValue: TextView = findViewById(R.id.homeAccessibilitySummaryValue)
        val accessibilityEffectiveValue: TextView = findViewById(R.id.homeAccessibilityEffectiveValue)
        val screenshotSummaryValue: TextView = findViewById(R.id.homeScreenshotSummaryValue)
        val screenshotEffectiveValue: TextView = findViewById(R.id.homeScreenshotEffectiveValue)

        val accessibilityRow = HomeCapabilityRowViews(accessibilitySummaryValue, accessibilityEffectiveValue)
        val screenshotRow = HomeCapabilityRowViews(screenshotSummaryValue, screenshotEffectiveValue)

        val diagnosticsBuildValue: TextView = findViewById(R.id.homeDiagnosticsBuildValue)
        val diagnosticsLastActionValue: TextView = findViewById(R.id.homeDiagnosticsLastActionValue)
        val diagnosticsForegroundValue: TextView = findViewById(R.id.homeDiagnosticsForegroundValue)

        val managePermissionsButton: Button = findViewById(R.id.openPermissionsButton)
        val openDiagnosticsButton: Button = findViewById(R.id.openDiagnosticsButton)
        val rootEntryButton: Button = findViewById(R.id.rootEntryButton)

        runtimeViewModel.homeScreenState.observe(this) { state ->
            renderHome(
                state,
                versionBadge,
                runtimeStatusValue,
                runtimeApiChip,
                runtimeServiceChip,
                runtimeAccessibilityChip,
                startRuntimeButton,
                permissionSummaryValue,
                accessibilityRow,
                screenshotRow,
                diagnosticsBuildValue,
                diagnosticsLastActionValue,
                diagnosticsForegroundValue,
                rootEntryButton
            )
        }

        startRuntimeButton.setOnClickListener {
            runtimeViewModel.requestForegroundServiceStart()
            ContextCompat.startForegroundService(this, Intent(this, GhosthandForegroundService::class.java))
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
        }

        updateButton.setOnClickListener {
            openExternalUrl(getString(R.string.home_update_url))
        }

        githubButton.setOnClickListener {
            openExternalUrl(getString(R.string.home_github_url))
        }

        managePermissionsButton.setOnClickListener {
            startActivity(PermissionsActivity.createIntent(this))
        }

        openDiagnosticsButton.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        rootEntryButton.setOnClickListener {
            startActivity(PermissionsActivity.createRootIntent(this))
        }
    }

    private fun renderHome(
        state: HomeScreenUiState,
        versionBadge: TextView,
        runtimeStatusValue: TextView,
        runtimeApiChip: TextView,
        runtimeServiceChip: TextView,
        runtimeAccessibilityChip: TextView,
        startRuntimeButton: Button,
        permissionSummaryValue: TextView,
        accessibilityRow: HomeCapabilityRowViews,
        screenshotRow: HomeCapabilityRowViews,
        diagnosticsBuildValue: TextView,
        diagnosticsLastActionValue: TextView,
        diagnosticsForegroundValue: TextView,
        rootEntryButton: Button
    ) {
        versionBadge.text = state.versionBadgeText
        UiStatusSupport.styleChip(this, versionBadge, StatusTone.Neutral)
        runtimeStatusValue.text = state.runtimeSummary.statusText
        runtimeApiChip.text = state.runtimeSummary.apiStatusText
        runtimeServiceChip.text = state.runtimeSummary.serviceStatusText
        runtimeAccessibilityChip.text = state.runtimeSummary.accessibilityStatusText
        UiStatusSupport.styleChip(this, runtimeApiChip, state.runtimeSummary.apiTone)
        UiStatusSupport.styleChip(this, runtimeServiceChip, state.runtimeSummary.serviceTone)
        UiStatusSupport.styleChip(this, runtimeAccessibilityChip, state.runtimeSummary.accessibilityTone)

        startRuntimeButton.isEnabled = state.runtimeSummary.actionEnabled
        startRuntimeButton.text = state.runtimeSummary.actionLabel
        permissionSummaryValue.text = state.permissionsSummaryText

        bindHomeCapabilityRow(accessibilityRow, state.accessibilitySummary)
        bindHomeCapabilityRow(screenshotRow, state.screenshotSummary)

        diagnosticsBuildValue.text = state.diagnosticsSummary.buildText
        diagnosticsLastActionValue.text = state.diagnosticsSummary.lastActionText
        diagnosticsForegroundValue.text = state.diagnosticsSummary.foregroundText
        rootEntryButton.text = state.rootEntryLabel
    }

    private fun bindHomeCapabilityRow(
        views: HomeCapabilityRowViews,
        uiState: HomeCapabilitySummaryUiState
    ) {
        views.summaryView.text = uiState.detailText
        views.effectiveView.text = uiState.effectiveText
        UiStatusSupport.styleChip(this, views.effectiveView, uiState.effectiveTone)
    }

    private fun openExternalUrl(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, R.string.home_external_link_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, R.string.home_external_link_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private data class HomeCapabilityRowViews(
        val summaryView: TextView,
        val effectiveView: TextView
    )
}
