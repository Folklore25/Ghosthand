/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class RuntimeState(
    val appStarted: Boolean = false,
    val appStartedAtElapsedRealtimeMs: Long? = null,
    val appStartedAtIso: String? = null,
    val buildVersion: String = "unknown",
    val installIdentity: String = "unknown",
    val foregroundPackage: String? = null,
    val localApiServerRunning: Boolean = false,
    val accessibilityServiceConnected: Boolean = false,
    val accessibilityDispatchCapable: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val accessibilityHealthy: Boolean? = null,
    val accessibilityStatus: String = "disabled",
    val screenshotPermissionGranted: Boolean = false,
    val capabilityPolicy: CapabilityPolicySnapshot = CapabilityPolicySnapshot(),
    val capabilityAccess: CapabilityAccessSnapshot = CapabilityAccessSnapshot(),
    val foregroundServiceRunning: Boolean = false,
    val tapProbeCount: Int = 0,
    val tapProbeUiBuildState: String = "unknown",
    val swipeProbeScrollY: Int = 0,
    val swipeProbeTopVisibleItem: Int = 1,
    val swipeProbeSignalText: String = "Swipe probe scrollY: 0 | top item: 01",
    val writeSecureSettingsGranted: Boolean? = null,
    val lastServiceAction: String = "",
    val lastAccessibilityHelperResult: String = "",
    val tapProbeResultText: String = "Tap probe count: 0",
    val statusText: String = "Ghosthand runtime placeholder is idle."
)
