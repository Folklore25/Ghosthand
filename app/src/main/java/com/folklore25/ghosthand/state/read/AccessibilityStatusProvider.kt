/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.read

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

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import org.json.JSONObject

class AccessibilityStatusProvider(
    private val context: Context
) {
    fun snapshot(
        isConnected: Boolean,
        isDispatchCapable: Boolean
    ): AccessibilityStatusSnapshot {
        val serviceEnabled = isServiceEnabled()
        val healthy = when {
            !serviceEnabled -> false
            isDispatchCapable -> true
            else -> null
        }

        val status = when {
            !serviceEnabled -> "disabled"
            isDispatchCapable -> "enabled_connected"
            else -> "enabled_idle"
        }

        return AccessibilityStatusSnapshot(
            implemented = true,
            enabled = serviceEnabled,
            connected = isConnected,
            dispatchCapable = isDispatchCapable,
            healthy = healthy,
            status = status
        )
    }

    fun toJson(snapshot: AccessibilityStatusSnapshot): JSONObject {
        return JSONObject()
            .put("implemented", snapshot.implemented)
            .put("enabled", snapshot.enabled)
            .put("connected", snapshot.connected)
            .put("dispatchCapable", snapshot.dispatchCapable)
            .put("healthy", snapshot.healthy ?: JSONObject.NULL)
            .put("status", snapshot.status)
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityGloballyEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        if (!accessibilityGloballyEnabled) {
            return false
        }

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val acceptedNames = GhostAccessibilityServiceComponents.primaryAcceptedEnabledNames(context)

        return enabledServices
            .split(':')
            .map(String::trim)
            .any { enabledName ->
                acceptedNames.any { acceptedName ->
                    enabledName.equals(acceptedName, ignoreCase = true)
                }
            }
    }
}

data class AccessibilityStatusSnapshot(
    val implemented: Boolean,
    val enabled: Boolean,
    val connected: Boolean,
    val dispatchCapable: Boolean,
    val healthy: Boolean?,
    val status: String
)
