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
}
