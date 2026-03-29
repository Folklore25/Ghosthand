/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

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
    val mediaProjectionGranted: Boolean = false,
    val rootFallbackAvailable: Boolean = false
) {
    val authorized: Boolean
        get() = accessibilityCaptureReady || mediaProjectionGranted || rootFallbackAvailable
}

data class RootSystemAuthorizationState(
    val available: Boolean? = null,
    val healthy: Boolean? = null,
    val status: String = "unknown"
) {
    val authorized: Boolean
        get() = available == true
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
    ),
    val root: GovernedCapabilitySnapshot<RootSystemAuthorizationState> = GovernedCapabilitySnapshot(
        system = RootSystemAuthorizationState()
    )
) {
    fun gateStateFor(capability: GhosthandCapability): CapabilityGateState {
        val snapshot = when (capability) {
            GhosthandCapability.Accessibility -> accessibility
            GhosthandCapability.Screenshot -> screenshot
            GhosthandCapability.Root -> root
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
        rootAvailability: RootAvailabilitySnapshot,
        policy: CapabilityPolicySnapshot
    ): CapabilityAccessSnapshot {
        val accessibilitySystem = AccessibilitySystemAuthorizationState(
            enabled = accessibilityStatus.enabled,
            connected = accessibilityStatus.connected,
            dispatchCapable = accessibilityStatus.dispatchCapable,
            healthy = accessibilityStatus.healthy,
            status = accessibilityStatus.status
        )
        val rootSystem = RootSystemAuthorizationState(
            available = rootAvailability.available,
            healthy = rootAvailability.healthy,
            status = rootAvailability.status
        )
        val screenshotSystem = ScreenshotSystemAuthorizationState(
            accessibilityCaptureReady = accessibilitySystem.dispatchCapable,
            mediaProjectionGranted = mediaProjectionGranted,
            rootFallbackAvailable = rootSystem.available == true
        )
        val accessibilityPolicy = AppCapabilityPolicyState(policy.accessibilityAllowed)
        val screenshotPolicy = AppCapabilityPolicyState(policy.screenshotAllowed)
        val rootPolicy = AppCapabilityPolicyState(policy.rootAllowed)
        val rootEffective = rootPolicy.allowed && rootSystem.authorized
        val screenshotEffective = screenshotPolicy.allowed && (
            screenshotSystem.accessibilityCaptureReady ||
                screenshotSystem.mediaProjectionGranted ||
                (screenshotSystem.rootFallbackAvailable && rootPolicy.allowed)
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
                        screenshotSystem.rootFallbackAvailable && rootPolicy.allowed -> "root_fallback"
                        else -> "system_missing"
                    }
                )
            ),
            root = GovernedCapabilitySnapshot(
                system = rootSystem,
                policy = rootPolicy,
                effective = CapabilityEffectiveState(
                    usableNow = rootEffective,
                    reason = when {
                        !rootPolicy.allowed -> "policy_blocked"
                        rootSystem.authorized -> "root_available"
                        rootSystem.status == "authorization_required" -> "authorization_required"
                        else -> "system_missing"
                    }
                )
            )
        )
    }
}
