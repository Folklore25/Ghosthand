/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class DiagnosticsActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        AppTextResolver.initialize(this)
        RuntimeStateStore.refreshLocalizedUiText(this)
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTextResolver.initialize(this)
        setContentView(R.layout.activity_diagnostics)

        val buildVersionValue: TextView = findViewById(R.id.diagnosticsBuildVersionValue)
        val installIdentityValue: TextView = findViewById(R.id.diagnosticsInstallIdentityValue)
        val lastServiceActionValue: TextView = findViewById(R.id.diagnosticsLastServiceActionValue)
        val runtimeStatusValue: TextView = findViewById(R.id.diagnosticsRuntimeStatusValue)
        val apiServerValue: TextView = findViewById(R.id.diagnosticsApiServerValue)
        val serviceValue: TextView = findViewById(R.id.diagnosticsServiceValue)
        val accessibilityValue: TextView = findViewById(R.id.diagnosticsAccessibilityValue)
        val screenshotValue: TextView = findViewById(R.id.diagnosticsScreenshotValue)

        findViewById<TextView>(R.id.diagnosticsInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Diagnostics)
        }

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        runtimeViewModel.runtimeState.observe(this) { state ->
            buildVersionValue.text = localizeValue(state.buildVersion)
            installIdentityValue.text = localizeValue(state.installIdentity)
            lastServiceActionValue.text = if (state.lastServiceAction.isBlank()) {
                getString(R.string.last_service_action_default)
            } else {
                state.lastServiceAction
            }
            runtimeStatusValue.text = state.statusText

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
