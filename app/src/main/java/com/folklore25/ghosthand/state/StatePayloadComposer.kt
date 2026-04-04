/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

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

import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation
import com.folklore25.ghosthand.capability.GovernedCapabilityPayloads
import com.folklore25.ghosthand.server.LocalApiServer
import org.json.JSONObject

object StatePayloadComposer {
    fun createStatePayload(
        runtimeState: RuntimeState,
        runtimeReady: Boolean,
        runtimeUptimeMs: Long?,
        diagnosticsSnapshot: HomeDiagnosticsSnapshot,
        deviceSnapshot: DeviceSnapshot,
        foregroundSnapshot: ForegroundAppSnapshot,
        accessibilitySnapshot: AccessibilityStatusSnapshot,
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): JSONObject {
        return JSONObject()
            .put("runtime", JSONObject()
                .put("ready", runtimeReady)
                .put("runtimeUptimeMs", runtimeUptimeMs ?: JSONObject.NULL)
                .put("appStartedAt", runtimeState.appStartedAtIso ?: JSONObject.NULL)
                .put("buildVersion", diagnosticsSnapshot.buildVersion)
                .put("installIdentity", diagnosticsSnapshot.installIdentity)
                .put("tapProbeUiBuildState", diagnosticsSnapshot.tapProbeUiBuildState)
                .put("foregroundServiceRunning", runtimeState.foregroundServiceRunning)
                .put("appStarted", runtimeState.appStarted)
                .put("lastServiceAction", runtimeState.lastServiceAction)
                .put("statusText", runtimeState.statusText)
            )
            .put("accessibility", JSONObject()
                .put("implemented", accessibilitySnapshot.implemented)
                .put("enabled", accessibilitySnapshot.enabled)
                .put("connected", accessibilitySnapshot.connected)
                .put("dispatchCapable", accessibilitySnapshot.dispatchCapable)
                .put("healthy", accessibilitySnapshot.healthy ?: JSONObject.NULL)
                .put("status", accessibilitySnapshot.status)
            )
            .put("device", JSONObject()
                .put("screenOn", deviceSnapshot.screenOn)
                .put("locked", deviceSnapshot.locked ?: JSONObject.NULL)
                .put("rotation", deviceSnapshot.rotation)
                .put("batteryPercent", deviceSnapshot.batteryPercent)
                .put("charging", deviceSnapshot.charging)
                .put("foregroundPackage", foregroundSnapshot.packageName ?: JSONObject.NULL)
            )
            .put("openclaw", JSONObject()
                .put("apiServerReady", runtimeState.localApiServerRunning)
                .put("port", LocalApiServer.PORT)
            )
            .put("recovery", JSONObject()
                .put("implemented", false)
                .put("lastAction", JSONObject.NULL)
                .put("lastResult", JSONObject.NULL)
                .put("status", "not_implemented")
            )
            .put(
                "permissions",
                JSONObject(
                    permissionsPayload(
                        accessibilityEnabled = accessibilitySnapshot.enabled,
                        capabilityAccess = capabilityAccess,
                        permissionSnapshot = permissionSnapshot
                    )
                )
            )
            .put("systemPermissions", JSONObject(systemPermissionsPayload(permissionSnapshot)))
    }

    fun permissionsPayload(
        accessibilityEnabled: Boolean,
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "accessibility" to accessibilityEnabled,
            "capabilitySummary" to GhosthandCapabilityPresentation.stateSummaryFields(
                capabilityAccess = capabilityAccess,
                permissionSnapshot = permissionSnapshot
            ),
            "capabilities" to linkedMapOf(
                "accessibility" to GovernedCapabilityPayloads.accessibilityToJson(capabilityAccess.accessibility),
                "screenshot" to GovernedCapabilityPayloads.screenshotToJson(capabilityAccess.screenshot)
            )
        )
    }

    fun systemPermissionsPayload(permissionSnapshot: PermissionSnapshot): Map<String, Any?> {
        return linkedMapOf(
            "usageAccess" to permissionSnapshot.usageAccess,
            "notifications" to permissionSnapshot.notifications,
            "overlay" to permissionSnapshot.overlay,
            "writeSecureSettings" to permissionSnapshot.writeSecureSettings
        )
    }
}
