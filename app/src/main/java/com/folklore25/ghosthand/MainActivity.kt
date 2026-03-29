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
        findViewById<android.widget.ImageButton>(R.id.homeUpdateInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Update)
        }
        findViewById<android.widget.ImageButton>(R.id.homeRuntimeInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Runtime)
        }
        findViewById<android.widget.ImageButton>(R.id.homePermissionsInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Permissions)
        }
        findViewById<android.widget.ImageButton>(R.id.homeDiagnosticsInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Diagnostics)
        }
        val updateInstalledValue: TextView = findViewById(R.id.homeUpdateInstalledValue)
        val updateLatestValue: TextView = findViewById(R.id.homeUpdateLatestValue)
        val updateStatusValue: TextView = findViewById(R.id.homeUpdateStatusValue)
        val updateButton: Button = findViewById(R.id.homeUpdateButton)
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
        val diagnosticsBuildValue: TextView = findViewById(R.id.homeDiagnosticsBuildValue)
        val diagnosticsLastActionValue: TextView = findViewById(R.id.homeDiagnosticsLastActionValue)
        val diagnosticsForegroundValue: TextView = findViewById(R.id.homeDiagnosticsForegroundValue)
        val managePermissionsButton: Button = findViewById(R.id.openPermissionsButton)
        val openDiagnosticsButton: Button = findViewById(R.id.openDiagnosticsButton)
        val binder = HomeScreenBinder(
            context = this,
            versionBadge = versionBadge,
            updateInstalledValue = updateInstalledValue,
            updateLatestValue = updateLatestValue,
            updateStatusValue = updateStatusValue,
            updateButton = updateButton,
            runtimeStatusValue = runtimeStatusValue,
            runtimeApiChip = runtimeApiChip,
            runtimeServiceChip = runtimeServiceChip,
            runtimeAccessibilityChip = runtimeAccessibilityChip,
            startRuntimeButton = startRuntimeButton,
            permissionSummaryValue = permissionSummaryValue,
            accessibilityRow = HomeCapabilityRowViews(accessibilitySummaryValue, accessibilityEffectiveValue),
            screenshotRow = HomeCapabilityRowViews(screenshotSummaryValue, screenshotEffectiveValue),
            diagnosticsBuildValue = diagnosticsBuildValue,
            diagnosticsLastActionValue = diagnosticsLastActionValue,
            diagnosticsForegroundValue = diagnosticsForegroundValue
        )

        runtimeViewModel.homeScreenState.observe(this) { state ->
            binder.bind(state)
        }

        startRuntimeButton.setOnClickListener {
            runtimeViewModel.requestForegroundServiceStart()
            ContextCompat.startForegroundService(this, Intent(this, GhosthandForegroundService::class.java))
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
        }

        updateButton.setOnClickListener {
            val actionUrl = runtimeViewModel.homeScreenState.value?.updateSummary?.actionUrl
            if (actionUrl == null) {
                runtimeViewModel.refreshReleaseInfo()
                Toast.makeText(this, R.string.home_update_refresh_started, Toast.LENGTH_SHORT).show()
            } else {
                openExternalUrl(actionUrl)
            }
        }

        managePermissionsButton.setOnClickListener {
            startActivity(PermissionsActivity.createIntent(this))
        }

        openDiagnosticsButton.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

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
}
