/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
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

    private val rootControlProvider = RootControlProvider()
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
    private lateinit var rootCard: android.view.View

    private lateinit var accessibilityCardViews: PermissionCardViews
    private lateinit var screenshotCardViews: PermissionCardViews
    private lateinit var rootCardViews: PermissionCardViews

    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        scrollView = findViewById(R.id.permissionsScroll)
        accessibilityCard = findViewById(R.id.accessibilityPermissionCard)
        screenshotCard = findViewById(R.id.screenshotPermissionCard)
        rootCard = findViewById(R.id.rootPermissionCard)

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
        rootCardViews = PermissionCardViews(
            systemView = findViewById(R.id.rootSystemStateChip),
            policyView = findViewById(R.id.rootPolicyValue),
            effectiveView = findViewById(R.id.rootEffectiveValue),
            policySwitch = findViewById(R.id.rootPolicySwitch),
            authorizeButton = findViewById(R.id.rootAuthorizeButton)
        )

        findViewById<Button>(R.id.permissionsBackButton).setOnClickListener { finish() }
        accessibilityCardViews.authorizeButton.setOnClickListener { openAccessibilitySettings() }
        screenshotCardViews.authorizeButton.setOnClickListener { requestScreenshotPermission() }
        rootCardViews.authorizeButton.setOnClickListener { checkRootAuthorization() }

        runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        runtimeViewModel.permissionsScreenState.observe(this) { state ->
            render(state)
        }

        when (intent.getStringExtra(EXTRA_FOCUS_CAPABILITY)) {
            FOCUS_ACCESSIBILITY -> scrollToCard(accessibilityCard)
            FOCUS_SCREENSHOT -> scrollToCard(screenshotCard)
            FOCUS_ROOT -> scrollToCard(rootCard)
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
        renderPermissionCard(
            views = rootCardViews,
            capability = GhosthandCapability.Root,
            uiState = runtimeState.root
        )
    }

    private fun openAccessibilitySettings() {
        val serviceComponent = GhostAccessibilityServiceComponents.primaryComponentName(this)
        val accessibilityDetailsIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
        }

        if (launchSettingsIntent(accessibilityDetailsIntent)) return
        if (launchSettingsIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) return

        Toast.makeText(this, R.string.accessibility_settings_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun requestScreenshotPermission() {
        val state = RuntimeStateStore.snapshot()
        if (!state.foregroundServiceRunning) {
            RuntimeStateStore.markServiceStartRequested()
            ContextCompat.startForegroundService(this, Intent(this, GhosthandForegroundService::class.java))
            Toast.makeText(this, R.string.service_requested, Toast.LENGTH_SHORT).show()
            window.decorView.postDelayed({ launchScreenshotConsent() }, FOREGROUND_SERVICE_WARMUP_MS)
            return
        }
        launchScreenshotConsent()
    }

    private fun launchScreenshotConsent() {
        screenshotPermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun checkRootAuthorization() {
        rootCardViews.authorizeButton.isEnabled = false
        Thread {
            val availability = rootControlProvider.availability()
            runOnUiThread {
                RuntimeStateStore.refreshRuntimeSnapshot(this)
                rootCardViews.authorizeButton.isEnabled = true
                val message = when (availability.status) {
                    "available" -> R.string.root_check_available
                    "authorization_required" -> R.string.root_check_authorization_required
                    else -> R.string.root_check_unavailable
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun launchSettingsIntent(intent: Intent): Boolean {
        return try {
            if (intent.resolveActivity(packageManager) == null) {
                false
            } else {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        } catch (_: Exception) {
            false
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
        private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
        private const val FOREGROUND_SERVICE_WARMUP_MS = 400L
        private const val EXTRA_FOCUS_CAPABILITY = "focus_capability"
        private const val FOCUS_ACCESSIBILITY = "accessibility"
        private const val FOCUS_SCREENSHOT = "screenshot"
        private const val FOCUS_ROOT = "root"

        fun createIntent(context: Context, focusCapability: String? = null): Intent {
            return Intent(context, PermissionsActivity::class.java).apply {
                putExtra(EXTRA_FOCUS_CAPABILITY, focusCapability)
            }
        }

        fun createRootIntent(context: Context): Intent = createIntent(context, FOCUS_ROOT)
    }

    private data class PermissionCardViews(
        val systemView: TextView,
        val policyView: TextView,
        val effectiveView: TextView,
        val policySwitch: SwitchMaterial,
        val authorizeButton: Button
    )
}
