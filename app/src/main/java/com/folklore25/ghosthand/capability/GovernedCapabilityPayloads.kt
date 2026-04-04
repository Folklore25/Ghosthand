/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

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
