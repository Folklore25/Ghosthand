/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Intent
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
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]

        val runtimeStatusValue: TextView = findViewById(R.id.homeRuntimeStatusValue)
        val runtimeApiChip: TextView = findViewById(R.id.homeApiStatusValue)
        val runtimeServiceChip: TextView = findViewById(R.id.homeServiceStatusValue)
        val runtimeAccessibilityChip: TextView = findViewById(R.id.homeAccessibilityStatusValue)
        val startRuntimeButton: Button = findViewById(R.id.startServiceButton)

        val accessibilitySystemChip: TextView = findViewById(R.id.homeAccessibilitySystemValue)
        val accessibilityPolicyChip: TextView = findViewById(R.id.homeAccessibilityPolicyValue)
        val screenshotSystemChip: TextView = findViewById(R.id.homeScreenshotSystemValue)
        val screenshotPolicyChip: TextView = findViewById(R.id.homeScreenshotPolicyValue)
        val rootSystemChip: TextView = findViewById(R.id.homeRootSystemValue)
        val rootPolicyChip: TextView = findViewById(R.id.homeRootPolicyValue)
        val permissionSummaryValue: TextView = findViewById(R.id.homePermissionSummaryValue)

        val diagnosticsBuildValue: TextView = findViewById(R.id.homeDiagnosticsBuildValue)
        val diagnosticsLastActionValue: TextView = findViewById(R.id.homeDiagnosticsLastActionValue)
        val diagnosticsForegroundValue: TextView = findViewById(R.id.homeDiagnosticsForegroundValue)

        val managePermissionsButton: Button = findViewById(R.id.openPermissionsButton)
        val openDiagnosticsButton: Button = findViewById(R.id.openDiagnosticsButton)
        val rootEntryButton: Button = findViewById(R.id.rootEntryButton)

        runtimeViewModel.runtimeState.observe(this) { state ->
            renderHome(
                state = state,
                policies = CapabilityPolicyStore.snapshot(),
                runtimeStatusValue = runtimeStatusValue,
                runtimeApiChip = runtimeApiChip,
                runtimeServiceChip = runtimeServiceChip,
                runtimeAccessibilityChip = runtimeAccessibilityChip,
                startRuntimeButton = startRuntimeButton,
                accessibilitySystemChip = accessibilitySystemChip,
                accessibilityPolicyChip = accessibilityPolicyChip,
                screenshotSystemChip = screenshotSystemChip,
                screenshotPolicyChip = screenshotPolicyChip,
                rootSystemChip = rootSystemChip,
                rootPolicyChip = rootPolicyChip,
                permissionSummaryValue = permissionSummaryValue,
                diagnosticsBuildValue = diagnosticsBuildValue,
                diagnosticsLastActionValue = diagnosticsLastActionValue,
                diagnosticsForegroundValue = diagnosticsForegroundValue,
                rootEntryButton = rootEntryButton
            )
        }
        CapabilityPolicyStore.observe().observe(this) { policies ->
            renderHome(
                state = RuntimeStateStore.snapshot(),
                policies = policies,
                runtimeStatusValue = runtimeStatusValue,
                runtimeApiChip = runtimeApiChip,
                runtimeServiceChip = runtimeServiceChip,
                runtimeAccessibilityChip = runtimeAccessibilityChip,
                startRuntimeButton = startRuntimeButton,
                accessibilitySystemChip = accessibilitySystemChip,
                accessibilityPolicyChip = accessibilityPolicyChip,
                screenshotSystemChip = screenshotSystemChip,
                screenshotPolicyChip = screenshotPolicyChip,
                rootSystemChip = rootSystemChip,
                rootPolicyChip = rootPolicyChip,
                permissionSummaryValue = permissionSummaryValue,
                diagnosticsBuildValue = diagnosticsBuildValue,
                diagnosticsLastActionValue = diagnosticsLastActionValue,
                diagnosticsForegroundValue = diagnosticsForegroundValue,
                rootEntryButton = rootEntryButton
            )
        }

        startRuntimeButton.setOnClickListener {
            runtimeViewModel.requestForegroundServiceStart()
            ContextCompat.startForegroundService(this, Intent(this, GhosthandForegroundService::class.java))
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
        }

        managePermissionsButton.setOnClickListener {
            startActivity(PermissionsActivity.createIntent(this))
        }

        openDiagnosticsButton.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        rootEntryButton.setOnClickListener {
            startActivity(PermissionsActivity.createIntent(this, CapabilityPolicy.RootCapability))
        }
    }

    private fun renderHome(
        state: RuntimeState,
        policies: CapabilityPolicyState,
        runtimeStatusValue: TextView,
        runtimeApiChip: TextView,
        runtimeServiceChip: TextView,
        runtimeAccessibilityChip: TextView,
        startRuntimeButton: Button,
        accessibilitySystemChip: TextView,
        accessibilityPolicyChip: TextView,
        screenshotSystemChip: TextView,
        screenshotPolicyChip: TextView,
        rootSystemChip: TextView,
        rootPolicyChip: TextView,
        permissionSummaryValue: TextView,
        diagnosticsBuildValue: TextView,
        diagnosticsLastActionValue: TextView,
        diagnosticsForegroundValue: TextView,
        rootEntryButton: Button
    ) {
        runtimeStatusValue.text = state.statusText
        runtimeApiChip.text = UiStatusSupport.booleanText(this, state.localApiServerRunning)
        runtimeServiceChip.text = UiStatusSupport.booleanText(this, state.foregroundServiceRunning)
        runtimeAccessibilityChip.text = UiStatusSupport.accessibilityStatusText(this, state.accessibilityStatus)
        UiStatusSupport.styleChip(this, runtimeApiChip, UiStatusSupport.booleanTone(state.localApiServerRunning))
        UiStatusSupport.styleChip(this, runtimeServiceChip, UiStatusSupport.booleanTone(state.foregroundServiceRunning))
        UiStatusSupport.styleChip(this, runtimeAccessibilityChip, UiStatusSupport.accessibilityTone(state.accessibilityStatus))

        startRuntimeButton.isEnabled = !state.foregroundServiceRunning
        startRuntimeButton.text = if (state.foregroundServiceRunning) {
            getString(R.string.service_button_running_label)
        } else {
            getString(R.string.service_button_label)
        }

        accessibilitySystemChip.text = UiStatusSupport.accessibilityStatusText(this, state.accessibilityStatus)
        screenshotSystemChip.text = if (state.screenshotPermissionGranted) {
            getString(R.string.permission_system_granted)
        } else {
            getString(R.string.permission_system_missing)
        }
        rootSystemChip.text = UiStatusSupport.rootStatusText(this, state.rootStatus)

        accessibilityPolicyChip.text = UiStatusSupport.policyStatusText(this, policies.accessibilityControlAllowed)
        screenshotPolicyChip.text = UiStatusSupport.policyStatusText(this, policies.screenshotCaptureAllowed)
        rootPolicyChip.text = UiStatusSupport.policyStatusText(this, policies.rootCapabilityAllowed)

        UiStatusSupport.styleChip(this, accessibilitySystemChip, UiStatusSupport.accessibilityTone(state.accessibilityStatus))
        UiStatusSupport.styleChip(this, screenshotSystemChip, UiStatusSupport.booleanTone(state.screenshotPermissionGranted))
        UiStatusSupport.styleChip(this, rootSystemChip, UiStatusSupport.rootTone(state.rootStatus))
        UiStatusSupport.styleChip(this, accessibilityPolicyChip, UiStatusSupport.policyTone(policies.accessibilityControlAllowed))
        UiStatusSupport.styleChip(this, screenshotPolicyChip, UiStatusSupport.policyTone(policies.screenshotCaptureAllowed))
        UiStatusSupport.styleChip(this, rootPolicyChip, UiStatusSupport.policyTone(policies.rootCapabilityAllowed))

        val allowedCount = listOf(
            policies.accessibilityControlAllowed,
            policies.screenshotCaptureAllowed,
            policies.rootCapabilityAllowed
        ).count { it }
        permissionSummaryValue.text = getString(R.string.home_permissions_summary_template, allowedCount, 3)

        diagnosticsBuildValue.text = localizeValue(state.buildVersion)
        diagnosticsLastActionValue.text = if (state.lastServiceAction.isBlank()) {
            getString(R.string.last_service_action_default)
        } else {
            state.lastServiceAction
        }
        diagnosticsForegroundValue.text = localizeValue(state.foregroundPackage)

        rootEntryButton.text = if (state.rootAvailable == true) {
            getString(R.string.home_root_entry_available)
        } else {
            getString(R.string.home_root_entry_default)
        }
    }

    private fun localizeValue(value: String?): String {
        return if (value.isNullOrBlank() || value == "unknown") {
            getString(R.string.runtime_placeholder_unknown)
        } else {
            value
        }
    }
}
