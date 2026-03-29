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

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val projection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            if (projection != null) {
                GhosthandServiceRegistry.getInstanceIfRunning()?.setMediaProjection(projection)
                RuntimeStateStore.refreshHomeDiagnostics(this)
                Toast.makeText(this, R.string.screenshot_permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.screenshot_permission_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            RuntimeStateStore.refreshHomeDiagnostics(this)
            Toast.makeText(this, R.string.screenshot_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var scrollView: ScrollView
    private lateinit var accessibilityCard: android.view.View
    private lateinit var screenshotCard: android.view.View
    private lateinit var rootCard: android.view.View

    private lateinit var accessibilitySystemChip: TextView
    private lateinit var accessibilityPolicyValue: TextView
    private lateinit var accessibilityPolicySwitch: SwitchMaterial
    private lateinit var accessibilityAuthorizeButton: Button

    private lateinit var screenshotSystemChip: TextView
    private lateinit var screenshotPolicyValue: TextView
    private lateinit var screenshotPolicySwitch: SwitchMaterial
    private lateinit var screenshotAuthorizeButton: Button

    private lateinit var rootSystemChip: TextView
    private lateinit var rootPolicyValue: TextView
    private lateinit var rootPolicySwitch: SwitchMaterial
    private lateinit var rootAuthorizeButton: Button

    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        scrollView = findViewById(R.id.permissionsScroll)
        accessibilityCard = findViewById(R.id.accessibilityPermissionCard)
        screenshotCard = findViewById(R.id.screenshotPermissionCard)
        rootCard = findViewById(R.id.rootPermissionCard)

        accessibilitySystemChip = findViewById(R.id.accessibilitySystemStateChip)
        accessibilityPolicyValue = findViewById(R.id.accessibilityPolicyValue)
        accessibilityPolicySwitch = findViewById(R.id.accessibilityPolicySwitch)
        accessibilityAuthorizeButton = findViewById(R.id.accessibilityAuthorizeButton)

        screenshotSystemChip = findViewById(R.id.screenshotSystemStateChip)
        screenshotPolicyValue = findViewById(R.id.screenshotPolicyValue)
        screenshotPolicySwitch = findViewById(R.id.screenshotPolicySwitch)
        screenshotAuthorizeButton = findViewById(R.id.screenshotAuthorizeButton)

        rootSystemChip = findViewById(R.id.rootSystemStateChip)
        rootPolicyValue = findViewById(R.id.rootPolicyValue)
        rootPolicySwitch = findViewById(R.id.rootPolicySwitch)
        rootAuthorizeButton = findViewById(R.id.rootAuthorizeButton)

        findViewById<Button>(R.id.permissionsBackButton).setOnClickListener { finish() }

        listOf(
            accessibilityPolicySwitch,
            screenshotPolicySwitch,
            rootPolicySwitch
        ).forEach { control ->
            control.isEnabled = false
            control.isClickable = false
            control.isFocusable = false
        }

        accessibilityAuthorizeButton.setOnClickListener { openAccessibilitySettings() }
        screenshotAuthorizeButton.setOnClickListener { requestScreenshotPermission() }
        rootAuthorizeButton.setOnClickListener { checkRootAuthorization() }

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        runtimeViewModel.runtimeState.observe(this) { state ->
            render(state)
        }

        when (intent.getStringExtra(EXTRA_FOCUS_CAPABILITY)) {
            FOCUS_ACCESSIBILITY -> scrollToCard(accessibilityCard)
            FOCUS_SCREENSHOT -> scrollToCard(screenshotCard)
            FOCUS_ROOT -> scrollToCard(rootCard)
        }
    }

    private fun render(runtimeState: RuntimeState) {
        accessibilityPolicySwitch.isChecked = runtimeState.accessibilityEnabled
        screenshotPolicySwitch.isChecked = runtimeState.screenshotPermissionGranted
        rootPolicySwitch.isChecked = runtimeState.rootAvailable == true

        accessibilityPolicyValue.text = if (runtimeState.accessibilityEnabled) {
            getString(R.string.permission_system_granted)
        } else {
            getString(R.string.permission_system_missing)
        }
        screenshotPolicyValue.text = if (runtimeState.screenshotPermissionGranted) {
            getString(R.string.permission_system_granted)
        } else {
            getString(R.string.permission_system_missing)
        }
        rootPolicyValue.text = if (runtimeState.rootAvailable == true) {
            getString(R.string.permission_system_granted)
        } else {
            getString(R.string.permission_system_missing)
        }

        accessibilitySystemChip.text = UiStatusSupport.accessibilityStatusText(this, runtimeState.accessibilityStatus)
        screenshotSystemChip.text = if (runtimeState.screenshotPermissionGranted) {
            getString(R.string.permission_system_granted)
        } else {
            getString(R.string.permission_system_missing)
        }
        rootSystemChip.text = UiStatusSupport.rootStatusText(this, runtimeState.rootStatus)

        UiStatusSupport.styleChip(this, accessibilitySystemChip, UiStatusSupport.accessibilityTone(runtimeState.accessibilityStatus))
        UiStatusSupport.styleChip(this, screenshotSystemChip, UiStatusSupport.booleanTone(runtimeState.screenshotPermissionGranted))
        UiStatusSupport.styleChip(this, rootSystemChip, UiStatusSupport.rootTone(runtimeState.rootStatus))
        UiStatusSupport.styleChip(this, accessibilityPolicyValue, UiStatusSupport.booleanTone(runtimeState.accessibilityEnabled))
        UiStatusSupport.styleChip(this, screenshotPolicyValue, UiStatusSupport.booleanTone(runtimeState.screenshotPermissionGranted))
        UiStatusSupport.styleChip(this, rootPolicyValue, UiStatusSupport.booleanTone(runtimeState.rootAvailable == true))

        accessibilityAuthorizeButton.text = if (runtimeState.accessibilityEnabled) {
            getString(R.string.permission_review_button)
        } else {
            getString(R.string.permission_authorize_accessibility_button)
        }
        screenshotAuthorizeButton.text = if (runtimeState.screenshotPermissionGranted) {
            getString(R.string.permission_review_button)
        } else {
            getString(R.string.permission_authorize_screenshot_button)
        }
        rootAuthorizeButton.text = if (runtimeState.rootAvailable == true) {
            getString(R.string.permission_refresh_root_button)
        } else {
            getString(R.string.permission_authorize_root_button)
        }
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
        rootAuthorizeButton.isEnabled = false
        Thread {
            val availability = rootControlProvider.availability()
            runOnUiThread {
                RuntimeStateStore.refreshHomeDiagnostics(this)
                rootAuthorizeButton.isEnabled = true
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
}
