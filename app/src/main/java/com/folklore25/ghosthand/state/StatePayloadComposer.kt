/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.capability.CapabilityAccessSnapshot
import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation
import com.folklore25.ghosthand.capability.GovernedCapabilityPayloads
import com.folklore25.ghosthand.state.device.DeviceSnapshot
import com.folklore25.ghosthand.state.device.ForegroundAppSnapshot
import com.folklore25.ghosthand.state.device.PermissionSnapshot
import com.folklore25.ghosthand.state.diagnostics.HomeDiagnosticsSnapshot
import com.folklore25.ghosthand.state.read.AccessibilityStatusSnapshot
import com.folklore25.ghosthand.state.runtime.RuntimeState
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
