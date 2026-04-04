/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.accessibility

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
import android.util.Log

class DevAccessibilityHelper(
    private val context: Context
) {
    private val permissionSnapshotProvider = PermissionSnapshotProvider(context.applicationContext)
    private val accessibilityStatusProvider = AccessibilityStatusProvider(context.applicationContext)

    fun isAvailable(): Boolean {
        return permissionSnapshotProvider.snapshot().writeSecureSettings == true
    }

    fun attemptEnableAccessibility(): DevAccessibilityHelperResult {
        if (!isAvailable()) {
            return DevAccessibilityHelperResult(
                available = false,
                success = false,
                resultText = context.getString(R.string.accessibility_helper_write_secure_settings_missing),
                postEnabled = null,
                postConnected = null,
                postStatus = null
            )
        }

        val componentName = GhostAccessibilityServiceComponents
            .primaryComponentName(context)
            .flattenToString()
        val managedServices = GhostAccessibilityServiceComponents.managedComponentNames(context)
        val existingServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        val updatedServices = existingServices
            .split(':')
            .filter { it.isNotBlank() }
            .filterNot { enabledName ->
                managedServices.any { managedName ->
                    enabledName.equals(managedName, ignoreCase = true)
                }
            }
            .toMutableSet()
            .apply { add(componentName) }
            .joinToString(":")

        val wroteServices = Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            updatedServices
        )
        val wroteGlobalEnabled = Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            1
        )

        val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
        val postSnapshot = accessibilityStatusProvider.snapshot(
            isConnected = executionStatus.connected,
            isDispatchCapable = executionStatus.dispatchCapable
        )
        val resultText = buildString {
            append(context.getString(R.string.accessibility_helper_attempt_prefix))
            append(' ')
            append(context.getString(R.string.accessibility_helper_wrote_services))
            append('=')
            append(wroteServices)
            append(", ")
            append(context.getString(R.string.accessibility_helper_accessibility_enabled))
            append('=')
            append(wroteGlobalEnabled)
            append(", ")
            append(context.getString(R.string.accessibility_helper_post_enabled))
            append('=')
            append(postSnapshot.enabled)
            append(", ")
            append(context.getString(R.string.accessibility_helper_connected))
            append('=')
            append(postSnapshot.connected)
            append(", ")
            append(context.getString(R.string.accessibility_helper_status))
            append('=')
            append(postSnapshot.status)
        }

        val success = postSnapshot.enabled
        Log.i(
            LOG_TAG,
            "event=dev_accessibility_helper available=true targetService=$componentName wroteServices=$wroteServices wroteAccessibilityEnabled=$wroteGlobalEnabled postEnabled=${postSnapshot.enabled} postConnected=${postSnapshot.connected} postStatus=${postSnapshot.status} success=$success"
        )

        return DevAccessibilityHelperResult(
            available = true,
            success = success,
            resultText = resultText,
            postEnabled = postSnapshot.enabled,
            postConnected = postSnapshot.connected,
            postStatus = postSnapshot.status
        )
    }

    private companion object {
        const val LOG_TAG = "GhostAccessibilityDev"
    }
}

data class DevAccessibilityHelperResult(
    val available: Boolean,
    val success: Boolean,
    val resultText: String,
    val postEnabled: Boolean?,
    val postConnected: Boolean?,
    val postStatus: String?
)
