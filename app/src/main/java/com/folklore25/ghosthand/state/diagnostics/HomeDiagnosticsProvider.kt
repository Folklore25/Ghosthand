/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.diagnostics

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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant

class HomeDiagnosticsProvider(
    private val context: Context
) {
    // Verification loop:
    // 1. install the latest APK
    // 2. force-stop Ghosthand
    // 3. explicitly start the activity and foreground service
    // 4. query /state and compare runtime.appStartedAt with runtime.installIdentity
    // If installIdentity is newer than appStartedAt, the running process predates the latest install.
    fun snapshot(): HomeDiagnosticsSnapshot {
        return try {
            val packageInfo = packageManager.getPackageInfoCompat(context.packageName)
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = packageInfo.longVersionCode
            HomeDiagnosticsSnapshot(
                buildVersion = "$versionName ($versionCode)",
                installIdentity = Instant.ofEpochMilli(packageInfo.lastUpdateTime).toString(),
                tapProbeUiBuildState = "Present in this build"
            )
        } catch (_: Exception) {
            HomeDiagnosticsSnapshot()
        }
    }

    private val packageManager: PackageManager
        get() = context.packageManager
}

data class HomeDiagnosticsSnapshot(
    val buildVersion: String = "unknown",
    val installIdentity: String = "unknown",
    val tapProbeUiBuildState: String = "unknown"
)

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoCompat(packageName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        getPackageInfo(packageName, 0)
    }
