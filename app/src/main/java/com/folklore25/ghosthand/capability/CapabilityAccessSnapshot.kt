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

data class AppCapabilityPolicyState(
    val allowed: Boolean = false
)

data class CapabilityEffectiveState(
    val usableNow: Boolean = false,
    val reason: String = "policy_blocked"
)

data class AccessibilitySystemAuthorizationState(
    val enabled: Boolean = false,
    val connected: Boolean = false,
    val dispatchCapable: Boolean = false,
    val healthy: Boolean? = null,
    val status: String = "disabled"
) {
    val authorized: Boolean
        get() = enabled
}

data class ScreenshotSystemAuthorizationState(
    val accessibilityCaptureReady: Boolean = false,
    val mediaProjectionGranted: Boolean = false
) {
    val authorized: Boolean
        get() = accessibilityCaptureReady || mediaProjectionGranted
}

data class GovernedCapabilitySnapshot<T>(
    val system: T,
    val policy: AppCapabilityPolicyState = AppCapabilityPolicyState(),
    val effective: CapabilityEffectiveState = CapabilityEffectiveState()
) {
    val policyAllowed: Boolean
        get() = policy.allowed

    val effectiveAvailable: Boolean
        get() = effective.usableNow

    val effectiveReason: String
        get() = effective.reason
}

data class CapabilityGateState(
    val policyAllowed: Boolean = false,
    val effectiveAvailable: Boolean = false,
    val effectiveReason: String = "policy_blocked"
)

data class CapabilityAccessSnapshot(
    val accessibility: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState> = GovernedCapabilitySnapshot(
        system = AccessibilitySystemAuthorizationState()
    ),
    val screenshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState> = GovernedCapabilitySnapshot(
        system = ScreenshotSystemAuthorizationState()
    )
) {
    fun gateStateFor(capability: GhosthandCapability): CapabilityGateState {
        val snapshot = when (capability) {
            GhosthandCapability.Accessibility -> accessibility
            GhosthandCapability.Screenshot -> screenshot
        }
        return CapabilityGateState(
            policyAllowed = snapshot.policyAllowed,
            effectiveAvailable = snapshot.effectiveAvailable,
            effectiveReason = snapshot.effectiveReason
        )
    }
}

object CapabilityAccessSnapshotFactory {
    fun create(
        accessibilityStatus: AccessibilityStatusSnapshot,
        mediaProjectionGranted: Boolean,
        policy: CapabilityPolicySnapshot
    ): CapabilityAccessSnapshot {
        val accessibilitySystem = AccessibilitySystemAuthorizationState(
            enabled = accessibilityStatus.enabled,
            connected = accessibilityStatus.connected,
            dispatchCapable = accessibilityStatus.dispatchCapable,
            healthy = accessibilityStatus.healthy,
            status = accessibilityStatus.status
        )
        val screenshotSystem = ScreenshotSystemAuthorizationState(
            accessibilityCaptureReady = accessibilitySystem.dispatchCapable,
            mediaProjectionGranted = mediaProjectionGranted
        )
        val accessibilityPolicy = AppCapabilityPolicyState(policy.accessibilityAllowed)
        val screenshotPolicy = AppCapabilityPolicyState(policy.screenshotAllowed)
        val screenshotEffective = screenshotPolicy.allowed && (
            screenshotSystem.accessibilityCaptureReady ||
                screenshotSystem.mediaProjectionGranted
        )

        return CapabilityAccessSnapshot(
            accessibility = GovernedCapabilitySnapshot(
                system = accessibilitySystem,
                policy = accessibilityPolicy,
                effective = CapabilityEffectiveState(
                    usableNow = accessibilityPolicy.allowed && accessibilitySystem.dispatchCapable,
                    reason = when {
                        !accessibilityPolicy.allowed -> "policy_blocked"
                        accessibilitySystem.dispatchCapable -> "accessibility_connected"
                        accessibilitySystem.enabled -> "service_idle"
                        else -> "system_missing"
                    }
                )
            ),
            screenshot = GovernedCapabilitySnapshot(
                system = screenshotSystem,
                policy = screenshotPolicy,
                effective = CapabilityEffectiveState(
                    usableNow = screenshotEffective,
                    reason = when {
                        !screenshotPolicy.allowed -> "policy_blocked"
                        screenshotSystem.mediaProjectionGranted -> "projection_granted"
                        screenshotSystem.accessibilityCaptureReady -> "accessibility_capture_ready"
                        else -> "system_missing"
                    }
                )
            )
        )
    }
}
