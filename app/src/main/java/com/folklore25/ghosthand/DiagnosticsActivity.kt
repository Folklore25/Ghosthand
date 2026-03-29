/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class DiagnosticsActivity : AppCompatActivity() {
    private val devAccessibilityHelper by lazy {
        DevAccessibilityHelper(this)
    }

    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        val buildVersionValue: TextView = findViewById(R.id.diagnosticsBuildVersionValue)
        val installIdentityValue: TextView = findViewById(R.id.diagnosticsInstallIdentityValue)
        val foregroundPackageValue: TextView = findViewById(R.id.diagnosticsForegroundPackageValue)
        val lastServiceActionValue: TextView = findViewById(R.id.diagnosticsLastServiceActionValue)
        val runtimeStatusValue: TextView = findViewById(R.id.diagnosticsRuntimeStatusValue)
        val apiServerValue: TextView = findViewById(R.id.diagnosticsApiServerValue)
        val serviceValue: TextView = findViewById(R.id.diagnosticsServiceValue)
        val accessibilityValue: TextView = findViewById(R.id.diagnosticsAccessibilityValue)
        val screenshotValue: TextView = findViewById(R.id.diagnosticsScreenshotValue)
        val helperResultValue: TextView = findViewById(R.id.diagnosticsHelperResultValue)
        val runHelperButton: com.google.android.material.button.MaterialButton = findViewById(R.id.diagnosticsRunHelperButton)

        findViewById<TextView>(R.id.diagnosticsInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Diagnostics)
        }

        runHelperButton.setOnClickListener {
            val result = devAccessibilityHelper.attemptEnableAccessibility()
            RuntimeStateStore.refreshRuntimeSnapshot(this)
            RuntimeStateStore.markAccessibilityHelperResult(result.resultText)
            Toast.makeText(this, result.resultText, Toast.LENGTH_SHORT).show()
        }

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        runtimeViewModel.runtimeState.observe(this) { state ->
            buildVersionValue.text = localizeValue(state.buildVersion)
            installIdentityValue.text = localizeValue(state.installIdentity)
            foregroundPackageValue.text = localizeValue(state.foregroundPackage)
            lastServiceActionValue.text = if (state.lastServiceAction.isBlank()) {
                getString(R.string.last_service_action_default)
            } else {
                state.lastServiceAction
            }
            runtimeStatusValue.text = state.statusText
            helperResultValue.text = if (state.lastAccessibilityHelperResult.isBlank()) {
                getString(R.string.accessibility_helper_result_default)
            } else {
                state.lastAccessibilityHelperResult
            }

            apiServerValue.text = UiStatusSupport.booleanText(this, state.localApiServerRunning)
            serviceValue.text = UiStatusSupport.booleanText(this, state.foregroundServiceRunning)
            accessibilityValue.text = UiStatusSupport.accessibilityStatusText(this, state.accessibilityStatus)
            screenshotValue.text = UiStatusSupport.screenshotSystemStatusText(this, state.capabilityAccess.screenshot.system)

            UiStatusSupport.styleChip(this, apiServerValue, UiStatusSupport.booleanTone(state.localApiServerRunning))
            UiStatusSupport.styleChip(this, serviceValue, UiStatusSupport.booleanTone(state.foregroundServiceRunning))
            UiStatusSupport.styleChip(this, accessibilityValue, UiStatusSupport.accessibilityTone(state.accessibilityStatus))
            UiStatusSupport.styleChip(
                this,
                screenshotValue,
                UiStatusSupport.screenshotSystemTone(state.capabilityAccess.screenshot.system)
            )

            runHelperButton.isEnabled = state.writeSecureSettingsGranted == true
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
