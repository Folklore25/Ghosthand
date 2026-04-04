/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.runtime

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
    val recoverableFailureAction: String? = null,
    val lastAccessibilityHelperResult: String = "",
    val tapProbeResultText: String = "Tap probe count: 0",
    val recoverableFailureStatus: String? = null,
    val statusText: String = "Ghosthand runtime placeholder is idle."
)
