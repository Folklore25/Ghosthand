/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

import com.folklore25.ghosthand.AccessibilitySystemAuthorizationState
import com.folklore25.ghosthand.GovernedCapabilitySnapshot
import com.folklore25.ghosthand.ScreenshotSystemAuthorizationState
import org.json.JSONObject

object GovernedCapabilityPayloads {
    fun accessibilityToJson(snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>): JSONObject {
        return JSONObject(accessibilityFields(snapshot))
    }

    fun screenshotToJson(snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>): JSONObject {
        return JSONObject(screenshotFields(snapshot))
    }

    fun accessibilityFields(
        snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "plane" to "control_and_observation",
            "directness" to "direct",
            "preconditions" to accessibilityPreconditions(),
            "failureModes" to accessibilityFailureModes(),
            "truthType" to "capability_gate_state",
            "system" to linkedMapOf(
                "authorized" to snapshot.system.authorized,
                "enabled" to snapshot.system.enabled,
                "connected" to snapshot.system.connected,
                "dispatchCapable" to snapshot.system.dispatchCapable,
                "healthy" to snapshot.system.healthy,
                "status" to snapshot.system.status
            ),
            "policy" to linkedMapOf("allowed" to snapshot.policy.allowed),
            "effective" to linkedMapOf(
                "usableNow" to snapshot.effective.usableNow,
                "reason" to snapshot.effective.reason
            )
        )
    }

    fun screenshotFields(
        snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "plane" to "preview",
            "directness" to "direct",
            "preconditions" to screenshotPreconditions(),
            "failureModes" to screenshotFailureModes(),
            "truthType" to "capability_gate_state",
            "system" to linkedMapOf(
                "authorized" to snapshot.system.authorized,
                "accessibilityCaptureReady" to snapshot.system.accessibilityCaptureReady,
                "mediaProjectionGranted" to snapshot.system.mediaProjectionGranted
            ),
            "policy" to linkedMapOf("allowed" to snapshot.policy.allowed),
            "effective" to linkedMapOf(
                "usableNow" to snapshot.effective.usableNow,
                "reason" to snapshot.effective.reason
            )
        )
    }

    fun accessibilitySummaryFields(
        snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "plane" to "control_and_observation",
            "allowed" to snapshot.policy.allowed,
            "usableNow" to snapshot.effective.usableNow,
            "reason" to snapshot.effective.reason,
            "preconditions" to accessibilityPreconditions(),
            "blockers" to accessibilityBlockers(snapshot),
            "failureModes" to accessibilityFailureModes()
        )
    }

    fun screenshotSummaryFields(
        snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "plane" to "preview",
            "allowed" to snapshot.policy.allowed,
            "usableNow" to snapshot.effective.usableNow,
            "reason" to snapshot.effective.reason,
            "preconditions" to screenshotPreconditions(),
            "blockers" to screenshotBlockers(snapshot),
            "failureModes" to screenshotFailureModes()
        )
    }

    private fun accessibilityPreconditions(): List<String> = listOf(
        "policy_allowed",
        "service_enabled",
        "service_connected",
        "dispatch_capable"
    )

    private fun accessibilityFailureModes(): List<String> = listOf(
        "policy_blocked",
        "accessibility_disabled",
        "accessibility_disconnected",
        "dispatch_unavailable"
    )

    private fun screenshotPreconditions(): List<String> = listOf(
        "policy_allowed",
        "accessibility_capture_ready_or_media_projection_granted"
    )

    private fun screenshotFailureModes(): List<String> = listOf(
        "policy_blocked",
        "capture_path_unavailable"
    )

    private fun accessibilityBlockers(
        snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>
    ): List<String> {
        return buildList {
            if (!snapshot.policy.allowed) add("policy_blocked")
            if (!snapshot.system.enabled) add("service_disabled")
            if (!snapshot.system.connected) add("service_disconnected")
            if (!snapshot.system.dispatchCapable) add("dispatch_unavailable")
            if (snapshot.system.healthy == false) add("service_unhealthy")
        }
    }

    private fun screenshotBlockers(
        snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>
    ): List<String> {
        return buildList {
            if (!snapshot.policy.allowed) add("policy_blocked")
            if (!snapshot.system.accessibilityCaptureReady && !snapshot.system.mediaProjectionGranted) {
                add("capture_path_unavailable")
            }
        }
    }
}
