/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class CapabilityPolicy(
    val preferenceKey: String,
    val defaultAllowed: Boolean
) {
    AccessibilityControl("capability_accessibility_control_allowed", true),
    ScreenshotCapture("capability_screenshot_capture_allowed", false),
    RootCapability("capability_root_capability_allowed", false)
}

data class CapabilityPolicyState(
    val accessibilityControlAllowed: Boolean = CapabilityPolicy.AccessibilityControl.defaultAllowed,
    val screenshotCaptureAllowed: Boolean = CapabilityPolicy.ScreenshotCapture.defaultAllowed,
    val rootCapabilityAllowed: Boolean = CapabilityPolicy.RootCapability.defaultAllowed
) {
    fun isAllowed(policy: CapabilityPolicy): Boolean {
        return when (policy) {
            CapabilityPolicy.AccessibilityControl -> accessibilityControlAllowed
            CapabilityPolicy.ScreenshotCapture -> screenshotCaptureAllowed
            CapabilityPolicy.RootCapability -> rootCapabilityAllowed
        }
    }
}

object CapabilityPolicyStore {
    private const val PREFS_NAME = "ghosthand_capability_policy"

    @Volatile
    private var appContext: Context? = null

    private val policyState = MutableLiveData(CapabilityPolicyState())

    fun initialize(context: Context) {
        appContext = context.applicationContext
        policyState.value = readState()
    }

    fun observe(): LiveData<CapabilityPolicyState> = policyState

    fun snapshot(): CapabilityPolicyState = policyState.value ?: readState()

    fun isAllowed(policy: CapabilityPolicy): Boolean = snapshot().isAllowed(policy)

    fun setAllowed(policy: CapabilityPolicy, allowed: Boolean) {
        preferences().edit().putBoolean(policy.preferenceKey, allowed).apply()
        policyState.value = readState()
    }

    private fun readState(): CapabilityPolicyState {
        val prefs = preferences()
        return CapabilityPolicyState(
            accessibilityControlAllowed = prefs.getBoolean(
                CapabilityPolicy.AccessibilityControl.preferenceKey,
                CapabilityPolicy.AccessibilityControl.defaultAllowed
            ),
            screenshotCaptureAllowed = prefs.getBoolean(
                CapabilityPolicy.ScreenshotCapture.preferenceKey,
                CapabilityPolicy.ScreenshotCapture.defaultAllowed
            ),
            rootCapabilityAllowed = prefs.getBoolean(
                CapabilityPolicy.RootCapability.preferenceKey,
                CapabilityPolicy.RootCapability.defaultAllowed
            )
        )
    }

    private fun preferences() = requireNotNull(appContext) {
        "CapabilityPolicyStore must be initialized before use."
    }.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
