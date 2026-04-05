/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.permissions

import com.folklore25.ghosthand.ui.common.model.AppTextResolver
import com.folklore25.ghosthand.interaction.execution.GhostAccessibilityServiceComponents
import com.folklore25.ghosthand.capability.GhosthandCapability
import com.folklore25.ghosthand.service.runtime.GhosthandForegroundService
import com.folklore25.ghosthand.service.runtime.GhosthandServiceRegistry
import com.folklore25.ghosthand.ui.common.model.ModuleExplanation
import com.folklore25.ghosthand.ui.common.dialog.ModuleExplanationDialogFragment
import com.folklore25.ghosthand.state.runtime.RuntimeStateStore
import com.folklore25.ghosthand.ui.main.RuntimeStateViewModel
import com.folklore25.ghosthand.ui.common.model.UiStatusSupport

import com.folklore25.ghosthand.R

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial

class PermissionsActivity : AppCompatActivity() {
    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private lateinit var runtimeViewModel: RuntimeStateViewModel

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val projection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            if (projection != null) {
                GhosthandServiceRegistry.getInstanceIfRunning()?.setMediaProjection(projection)
                RuntimeStateStore.refreshRuntimeSnapshot(this)
                Toast.makeText(this, R.string.screenshot_permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.screenshot_permission_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            RuntimeStateStore.refreshRuntimeSnapshot(this)
            Toast.makeText(this, R.string.screenshot_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var scrollView: ScrollView
    private lateinit var accessibilityCard: android.view.View
    private lateinit var screenshotCard: android.view.View

    private lateinit var accessibilityCardViews: PermissionCardViews
    private lateinit var screenshotCardViews: PermissionCardViews

    override fun onResume() {
        super.onResume()
        AppTextResolver.initialize(this)
        RuntimeStateStore.refreshLocalizedUiText(this)
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTextResolver.initialize(this)
        setContentView(R.layout.activity_permissions)

        scrollView = findViewById(R.id.permissionsScroll)
        accessibilityCard = findViewById(R.id.accessibilityPermissionCard)
        screenshotCard = findViewById(R.id.screenshotPermissionCard)

        accessibilityCardViews = PermissionCardViews(
            systemView = findViewById(R.id.accessibilitySystemStateChip),
            policyView = findViewById(R.id.accessibilityPolicyValue),
            effectiveView = findViewById(R.id.accessibilityEffectiveValue),
            policySwitch = findViewById(R.id.accessibilityPolicySwitch),
            authorizeButton = findViewById(R.id.accessibilityAuthorizeButton)
        )
        screenshotCardViews = PermissionCardViews(
            systemView = findViewById(R.id.screenshotSystemStateChip),
            policyView = findViewById(R.id.screenshotPolicyValue),
            effectiveView = findViewById(R.id.screenshotEffectiveValue),
            policySwitch = findViewById(R.id.screenshotPolicySwitch),
            authorizeButton = findViewById(R.id.screenshotAuthorizeButton)
        )

        findViewById<TextView>(R.id.permissionsInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Permissions)
        }
        findViewById<TextView>(R.id.accessibilityInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Accessibility)
        }
        findViewById<TextView>(R.id.screenshotInfoButton).setOnClickListener {
            ModuleExplanationDialogFragment.show(supportFragmentManager, ModuleExplanation.Screenshot)
        }
        accessibilityCardViews.authorizeButton.setOnClickListener { openAccessibilitySettings() }
        screenshotCardViews.authorizeButton.setOnClickListener { requestScreenshotPermission() }

        runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        runtimeViewModel.permissionsScreenState.observe(this) { state ->
            render(state)
        }

        when (intent.getStringExtra(EXTRA_FOCUS_CAPABILITY)) {
            FOCUS_ACCESSIBILITY -> scrollToCard(accessibilityCard)
            FOCUS_SCREENSHOT -> scrollToCard(screenshotCard)
        }
    }

    private fun render(runtimeState: PermissionsScreenUiState) {
        renderPermissionCard(
            views = accessibilityCardViews,
            capability = GhosthandCapability.Accessibility,
            uiState = runtimeState.accessibility
        )
        renderPermissionCard(
            views = screenshotCardViews,
            capability = GhosthandCapability.Screenshot,
            uiState = runtimeState.screenshot
        )
    }

    private fun openAccessibilitySettings() {
        val serviceComponent = GhostAccessibilityServiceComponents.primaryComponentName(this)
        val accessibilityDetailsIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
        }

        val detailsResult = launchSettingsIntent("accessibility_details", accessibilityDetailsIntent)
        if (detailsResult.launched) return
        val genericResult = launchSettingsIntent("accessibility_settings", Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        if (genericResult.launched) return

        val failureClass = genericResult.failureClass ?: detailsResult.failureClass ?: "Unavailable"
        RuntimeStateStore.recordAccessibilitySettingsFailure(
            preconditions = "detailsFailure=${detailsResult.failureClass ?: "none"} genericFailure=${genericResult.failureClass ?: "none"}",
            failureClass = failureClass
        )
        Toast.makeText(this, R.string.accessibility_settings_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun requestScreenshotPermission() {
        val state = RuntimeStateStore.snapshot()
        if (!state.foregroundServiceRunning) {
            RuntimeStateStore.markServiceStartRequested()
            try {
                ContextCompat.startForegroundService(this, Intent(this, GhosthandForegroundService::class.java))
                Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
                window.decorView.postDelayed({ launchScreenshotConsent() }, FOREGROUND_SERVICE_WARMUP_MS)
            } catch (error: Exception) {
                RuntimeStateStore.recordRuntimeStartFailure(
                    preconditions = "serviceRunning=${state.foregroundServiceRunning} source=screenshot_permission",
                    error = error
                )
                Log.e(
                    LOG_TAG,
                    "event=start_runtime_failed source=screenshot_permission serviceRunning=${state.foregroundServiceRunning} failureClass=${error.javaClass.simpleName} fallback=skip_screenshot_consent",
                    error
                )
                Toast.makeText(this, R.string.runtime_start_failed_toast, Toast.LENGTH_LONG).show()
            }
            return
        }
        launchScreenshotConsent()
    }

    private fun launchScreenshotConsent() {
        screenshotPermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun launchSettingsIntent(label: String, intent: Intent): SettingsLaunchResult {
        return try {
            if (intent.resolveActivity(packageManager) == null) {
                Log.w(
                    LOG_TAG,
                    "event=settings_launch_unavailable action=$label failureClass=ResolveActivityMissing fallback=next_intent"
                )
                SettingsLaunchResult(false, "ResolveActivityMissing")
            } else {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                SettingsLaunchResult(true, null)
            }
        } catch (error: Exception) {
            Log.e(
                LOG_TAG,
                "event=settings_launch_failed action=$label failureClass=${error.javaClass.simpleName} fallback=next_intent",
                error
            )
            SettingsLaunchResult(false, error.javaClass.simpleName)
        }
    }

    private fun scrollToCard(card: android.view.View) {
        scrollView.post { scrollView.smoothScrollTo(0, card.top) }
    }

    private fun renderPermissionCard(
        views: PermissionCardViews,
        capability: GhosthandCapability,
        uiState: CapabilityPermissionUiState
    ) {
        views.policySwitch.setOnCheckedChangeListener(null)
        views.policySwitch.isChecked = uiState.policyAllowed
        views.policySwitch.setOnCheckedChangeListener { _, isChecked ->
            runtimeViewModel.setCapabilityPolicy(capability, isChecked)
        }

        views.systemView.text = uiState.systemLabel
        views.policyView.text = uiState.policyLabel
        views.effectiveView.text = uiState.effectiveLabel
        views.authorizeButton.text = uiState.authorizeLabel

        UiStatusSupport.styleChip(this, views.systemView, uiState.systemTone)
        UiStatusSupport.styleChip(this, views.policyView, uiState.policyTone)
        UiStatusSupport.styleChip(this, views.effectiveView, uiState.effectiveTone)
    }

    companion object {
        private const val LOG_TAG = "GhostBootstrap"
        private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
        private const val FOREGROUND_SERVICE_WARMUP_MS = 400L
        private const val EXTRA_FOCUS_CAPABILITY = "focus_capability"
        private const val FOCUS_ACCESSIBILITY = "accessibility"
        private const val FOCUS_SCREENSHOT = "screenshot"

        fun createIntent(context: Context, focusCapability: String? = null): Intent {
            return Intent(context, PermissionsActivity::class.java).apply {
                putExtra(EXTRA_FOCUS_CAPABILITY, focusCapability)
            }
        }
    }

    private data class PermissionCardViews(
        val systemView: TextView,
        val policyView: TextView,
        val effectiveView: TextView,
        val policySwitch: SwitchMaterial,
        val authorizeButton: Button
    )

    private data class SettingsLaunchResult(
        val launched: Boolean,
        val failureClass: String?
    )
}
