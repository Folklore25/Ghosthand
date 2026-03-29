/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

internal interface UiTextLookup {
    fun getString(resId: Int, vararg args: Any): String
}

internal object AppUiTextLookup : UiTextLookup {
    override fun getString(resId: Int, vararg args: Any): String {
        return AppTextResolver.getString(resId, *args)
    }
}

internal data class CapabilityPermissionUiState(
    val systemLabel: String,
    val systemTone: StatusTone,
    val policyLabel: String,
    val policyTone: StatusTone,
    val effectiveLabel: String,
    val effectiveTone: StatusTone,
    val policyAllowed: Boolean,
    val authorizeLabel: String
)

internal object CapabilityUiStateFactory {
    fun forCapability(
        capability: GhosthandCapability,
        runtimeState: RuntimeState,
        textLookup: UiTextLookup = AppUiTextLookup
    ): CapabilityPermissionUiState {
        return when (capability) {
            GhosthandCapability.Accessibility -> {
                val snapshot = runtimeState.capabilityAccess.accessibility
                CapabilityPermissionUiState(
                    systemLabel = UiStatusSupport.accessibilityStatusText(
                        textLookup,
                        snapshot.system.status
                    ),
                    systemTone = UiStatusSupport.accessibilityTone(snapshot.system.status),
                    policyLabel = UiStatusSupport.policyStatusText(textLookup, snapshot.policy.allowed),
                    policyTone = UiStatusSupport.policyTone(snapshot.policy.allowed),
                    effectiveLabel = UiStatusSupport.effectiveStatusText(textLookup, snapshot.effective),
                    effectiveTone = UiStatusSupport.effectiveTone(snapshot.effective.usableNow),
                    policyAllowed = snapshot.policy.allowed,
                    authorizeLabel = if (snapshot.system.enabled) {
                        textLookup.getString(R.string.permission_review_button)
                    } else {
                        textLookup.getString(R.string.permission_authorize_accessibility_button)
                    }
                )
            }

            GhosthandCapability.Screenshot -> {
                val snapshot = runtimeState.capabilityAccess.screenshot
                CapabilityPermissionUiState(
                    systemLabel = UiStatusSupport.screenshotSystemStatusText(
                        textLookup,
                        snapshot.system
                    ),
                    systemTone = UiStatusSupport.screenshotSystemTone(snapshot.system),
                    policyLabel = UiStatusSupport.policyStatusText(textLookup, snapshot.policy.allowed),
                    policyTone = UiStatusSupport.policyTone(snapshot.policy.allowed),
                    effectiveLabel = UiStatusSupport.effectiveStatusText(textLookup, snapshot.effective),
                    effectiveTone = UiStatusSupport.effectiveTone(snapshot.effective.usableNow),
                    policyAllowed = snapshot.policy.allowed,
                    authorizeLabel = if (snapshot.system.mediaProjectionGranted || snapshot.system.accessibilityCaptureReady) {
                        textLookup.getString(R.string.permission_review_button)
                    } else {
                        textLookup.getString(R.string.permission_authorize_screenshot_button)
                    }
                )
            }

            GhosthandCapability.Root -> {
                val snapshot = runtimeState.capabilityAccess.root
                CapabilityPermissionUiState(
                    systemLabel = UiStatusSupport.rootStatusText(textLookup, snapshot.system.status),
                    systemTone = UiStatusSupport.rootTone(snapshot.system.status),
                    policyLabel = UiStatusSupport.policyStatusText(textLookup, snapshot.policy.allowed),
                    policyTone = UiStatusSupport.policyTone(snapshot.policy.allowed),
                    effectiveLabel = UiStatusSupport.effectiveStatusText(textLookup, snapshot.effective),
                    effectiveTone = UiStatusSupport.effectiveTone(snapshot.effective.usableNow),
                    policyAllowed = snapshot.policy.allowed,
                    authorizeLabel = if (snapshot.system.authorized) {
                        textLookup.getString(R.string.permission_refresh_root_button)
                    } else {
                        textLookup.getString(R.string.permission_authorize_root_button)
                    }
                )
            }
        }
    }
}
