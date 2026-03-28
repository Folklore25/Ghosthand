/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.res.ColorStateList
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    private val devAccessibilityHelper by lazy {
        DevAccessibilityHelper(this)
    }

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val projection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            if (projection != null) {
                GhosthandServiceRegistry.getInstanceIfRunning()?.setMediaProjection(projection)
                Log.i(LOG_TAG, "event=media_projection_granted")
                Toast.makeText(this, R.string.screenshot_permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Log.w(LOG_TAG, "event=media_projection_null")
                Toast.makeText(this, R.string.screenshot_permission_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.i(LOG_TAG, "event=media_projection_denied resultCode=${result.resultCode}")
            Toast.makeText(this, R.string.screenshot_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingOperatorPostCheck: String? = null

    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        val pendingReason = pendingOperatorPostCheck ?: return
        pendingOperatorPostCheck = null
        val state = RuntimeStateStore.snapshot()
        Log.i(
            LOG_TAG,
            "event=operator_flow_post source=$pendingReason enabled=${state.accessibilityEnabled} connected=${state.accessibilityServiceConnected} status=${state.accessibilityStatus} rootStatus=${state.rootStatus} foregroundServiceRunning=${state.foregroundServiceRunning}"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]

        val buildVersionView: TextView = findViewById(R.id.buildVersionValue)
        val installIdentityView: TextView = findViewById(R.id.installIdentityValue)
        val writeSecureSettingsView: TextView = findViewById(R.id.writeSecureSettingsValue)
        val accessibilityStatusView: TextView = findViewById(R.id.accessibilityStatusValue)
        val accessibilityConnectedView: TextView = findViewById(R.id.accessibilityConnectedValue)
        val accessibilityHelperResultView: TextView = findViewById(R.id.accessibilityHelperResultValue)
        val rootSectionGroup: View = findViewById(R.id.rootSectionGroup)
        val rootStatusView: TextView = findViewById(R.id.rootStatusValue)
        val rootAvailableView: TextView = findViewById(R.id.rootAvailableValue)
        val appStartedView: TextView = findViewById(R.id.appStartedValue)
        val apiServerRunningView: TextView = findViewById(R.id.apiServerRunningValue)
        val serviceRunningView: TextView = findViewById(R.id.serviceRunningValue)
        val accessibilityEnabledView: TextView = findViewById(R.id.accessibilityEnabledValue)
        val dispatchCapableView: TextView = findViewById(R.id.dispatchCapableValue)
        val lastServiceActionView: TextView = findViewById(R.id.lastServiceActionValue)
        val runtimeStatusView: TextView = findViewById(R.id.runtimeStatusValue)
        val openAccessibilitySettingsButton: Button = findViewById(R.id.openAccessibilitySettingsButton)
        val runDevAccessibilityHelperButton: Button = findViewById(R.id.runDevAccessibilityHelperButton)
        val startServiceButton: Button = findViewById(R.id.startServiceButton)
        val grantScreenshotPermissionButton: Button = findViewById(R.id.grantScreenshotPermissionButton)

        runtimeViewModel.runtimeState.observe(this) { state ->
            buildVersionView.text = localizeValue(state.buildVersion)
            installIdentityView.text = localizeValue(state.installIdentity)
            writeSecureSettingsView.text = formatOptionalBoolean(state.writeSecureSettingsGranted)
            accessibilityStatusView.text = localizeAccessibilityStatus(state.accessibilityStatus)
            accessibilityConnectedView.text = formatBoolean(state.accessibilityServiceConnected)
            accessibilityHelperResultView.text = if (state.lastAccessibilityHelperResult.isBlank()) {
                getString(R.string.accessibility_helper_result_default)
            } else {
                state.lastAccessibilityHelperResult
            }
            rootSectionGroup.visibility = if (shouldShowRootSection(state)) View.VISIBLE else View.GONE
            rootStatusView.text = localizeRootStatus(state.rootStatus)
            rootAvailableView.text = formatOptionalBoolean(state.rootAvailable)
            appStartedView.text = formatBoolean(state.appStarted)
            apiServerRunningView.text = formatBoolean(state.localApiServerRunning)
            serviceRunningView.text = formatBoolean(state.foregroundServiceRunning)
            accessibilityEnabledView.text = formatBoolean(state.accessibilityEnabled)
            dispatchCapableView.text = formatBoolean(state.accessibilityDispatchCapable)
            lastServiceActionView.text = if (state.lastServiceAction.isBlank() || state.lastServiceAction == "Not started") {
                getString(R.string.last_service_action_default)
            } else {
                state.lastServiceAction
            }
            runtimeStatusView.text = state.statusText
            runDevAccessibilityHelperButton.isEnabled = state.writeSecureSettingsGranted == true

            styleChip(accessibilityStatusView, statusTone(state.accessibilityStatus))
            styleChip(accessibilityConnectedView, booleanTone(state.accessibilityServiceConnected))
            styleChip(rootStatusView, rootTone(state.rootStatus))
            styleChip(rootAvailableView, optionalBooleanTone(state.rootAvailable))
            styleChip(apiServerRunningView, booleanTone(state.localApiServerRunning))
            styleChip(serviceRunningView, booleanTone(state.foregroundServiceRunning))
            styleChip(accessibilityEnabledView, booleanTone(state.accessibilityEnabled))
            styleChip(dispatchCapableView, booleanTone(state.accessibilityDispatchCapable))
            styleChip(appStartedView, booleanTone(state.appStarted))
            styleChip(writeSecureSettingsView, optionalBooleanTone(state.writeSecureSettingsGranted))
        }

        openAccessibilitySettingsButton.setOnClickListener {
            runWithForegroundService("open_accessibility_settings", runtimeViewModel) {
                openAccessibilitySettings()
            }
        }

        runDevAccessibilityHelperButton.setOnClickListener {
            runWithForegroundService("run_dev_accessibility_helper", runtimeViewModel) {
                runDevelopmentAccessibilityHelper()
            }
        }

        startServiceButton.setOnClickListener {
            runtimeViewModel.requestForegroundServiceStart()
            val serviceIntent = Intent(this, GhosthandForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
        }

        grantScreenshotPermissionButton.setOnClickListener {
            requestScreenshotPermission()
        }
    }

    private fun requestScreenshotPermission() {
        if (!RuntimeStateStore.snapshot().foregroundServiceRunning) {
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        screenshotPermissionLauncher.launch(intent)
    }

    private fun openAccessibilitySettings() {
        pendingOperatorPostCheck = "accessibility_settings"
        val serviceComponent = GhostAccessibilityServiceComponents.primaryComponentName(this)
        val accessibilityDetailsIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
        }

        if (launchSettingsIntent(accessibilityDetailsIntent, "accessibility_details")) {
            return
        }

        val accessibilitySettingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (launchSettingsIntent(accessibilitySettingsIntent, "accessibility_settings")) {
            return
        }

        Log.w(LOG_TAG, "Failed to open accessibility settings shortcut")
        Toast.makeText(this, R.string.accessibility_settings_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun runDevelopmentAccessibilityHelper() {
        val result = devAccessibilityHelper.attemptEnableAccessibility()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        RuntimeStateStore.markAccessibilityHelperResult(result.resultText)
        val state = RuntimeStateStore.snapshot()
        Log.i(
            LOG_TAG,
            "event=operator_flow_post source=dev_accessibility_helper available=${result.available} success=${result.success} enabled=${state.accessibilityEnabled} connected=${state.accessibilityServiceConnected} status=${state.accessibilityStatus} rootStatus=${state.rootStatus} foregroundServiceRunning=${state.foregroundServiceRunning}"
        )
        Toast.makeText(this, result.resultText, Toast.LENGTH_SHORT).show()
    }

    private fun runWithForegroundService(
        reason: String,
        runtimeViewModel: RuntimeStateViewModel,
        action: () -> Unit
    ) {
        val state = RuntimeStateStore.snapshot()
        if (state.foregroundServiceRunning) {
            Log.i(LOG_TAG, "event=operator_flow reason=$reason foregroundService=already_running")
            action()
            return
        }

        Log.i(LOG_TAG, "event=operator_flow reason=$reason foregroundService=starting_now")
        runtimeViewModel.requestForegroundServiceStart()
        val serviceIntent = Intent(this, GhosthandForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        window.decorView.postDelayed({
            val updatedState = RuntimeStateStore.snapshot()
            Log.i(
                LOG_TAG,
                "event=operator_flow reason=$reason foregroundServicePostStart=${updatedState.foregroundServiceRunning}"
            )
            action()
        }, FOREGROUND_SERVICE_WARMUP_MS)
    }

    private fun launchSettingsIntent(intent: Intent, route: String): Boolean {
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            val canHandle = launchIntent.resolveActivity(packageManager) != null
            if (!canHandle) {
                Log.i(LOG_TAG, "Accessibility settings route unavailable: $route")
                return false
            }
            startActivity(launchIntent)
            Log.i(LOG_TAG, "Opened accessibility settings route: $route")
            true
        } catch (error: Exception) {
            Log.w(LOG_TAG, "Accessibility settings route failed: $route", error)
            false
        }
    }

    private fun formatBoolean(value: Boolean): String {
        return if (value) {
            getString(R.string.runtime_boolean_true)
        } else {
            getString(R.string.runtime_boolean_false)
        }
    }

    private fun formatOptionalBoolean(value: Boolean?): String {
        return when (value) {
            true -> getString(R.string.runtime_boolean_true)
            false -> getString(R.string.runtime_boolean_false)
            null -> getString(R.string.runtime_boolean_unknown)
        }
    }

    private fun localizeValue(value: String?): String {
        return if (value.isNullOrBlank() || value == "unknown") {
            getString(R.string.runtime_placeholder_unknown)
        } else {
            value
        }
    }

    private fun localizeAccessibilityStatus(status: String): String {
        return when (status) {
            "disabled" -> getString(R.string.accessibility_status_disabled)
            "enabled_idle" -> getString(R.string.accessibility_status_enabled_idle)
            "enabled_connected" -> getString(R.string.accessibility_status_enabled_connected)
            else -> status
        }
    }

    private fun localizeRootStatus(status: String): String {
        return when (status) {
            "available" -> getString(R.string.root_status_available)
            "authorization_required" -> getString(R.string.root_status_authorization_required)
            "unavailable" -> getString(R.string.root_status_unavailable)
            "unknown" -> getString(R.string.runtime_boolean_unknown)
            else -> status
        }
    }

    private fun shouldShowRootSection(state: RuntimeState): Boolean {
        return state.rootAvailable == true || state.rootStatus == "authorization_required"
    }

    private fun styleChip(view: TextView, tone: ChipTone) {
        view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, tone.backgroundRes))
        view.setTextColor(ContextCompat.getColor(this, tone.textRes))
    }

    private fun statusTone(status: String): ChipTone {
        return when (status) {
            "enabled_connected" -> ChipTone.Success
            "enabled_idle" -> ChipTone.Warning
            else -> ChipTone.Neutral
        }
    }

    private fun rootTone(status: String): ChipTone {
        return when (status) {
            "available" -> ChipTone.Success
            "authorization_required" -> ChipTone.Warning
            else -> ChipTone.Neutral
        }
    }

    private fun booleanTone(value: Boolean): ChipTone {
        return if (value) ChipTone.Success else ChipTone.Neutral
    }

    private fun optionalBooleanTone(value: Boolean?): ChipTone {
        return when (value) {
            true -> ChipTone.Success
            false -> ChipTone.Neutral
            null -> ChipTone.Neutral
        }
    }

    private companion object {
        const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
        const val LOG_TAG = "GhosthandHome"
        const val FOREGROUND_SERVICE_WARMUP_MS = 400L
    }
}

private enum class ChipTone(
    val backgroundRes: Int,
    val textRes: Int
) {
    Success(
        backgroundRes = R.color.gh_chip_success_bg,
        textRes = R.color.gh_chip_success_fg
    ),
    Warning(
        backgroundRes = R.color.gh_chip_warning_bg,
        textRes = R.color.gh_chip_warning_fg
    ),
    Neutral(
        backgroundRes = R.color.gh_chip_neutral_bg,
        textRes = R.color.gh_chip_neutral_fg
    )
}
