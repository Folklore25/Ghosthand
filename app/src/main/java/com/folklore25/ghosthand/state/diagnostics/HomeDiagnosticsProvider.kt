/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.diagnostics

import com.folklore25.ghosthand.R

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
