/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.device

import com.folklore25.ghosthand.R

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class PermissionSnapshotProvider(
    private val context: Context
) {
    fun snapshot(): PermissionSnapshot {
        return PermissionSnapshot(
            usageAccess = hasUsageAccess(),
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            overlay = canDrawOverlays(),
            writeSecureSettings = hasWriteSecureSettings()
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun hasWriteSecureSettings(): Boolean {
        val packageInfo = context.packageManager.getPackageInfoWithPermissions(context.packageName)
            ?: return false
        val permissions = packageInfo.requestedPermissions ?: return false
        val permissionIndex = permissions.indexOf(Manifest.permission.WRITE_SECURE_SETTINGS)
        if (permissionIndex == -1) {
            return false
        }

        val flags = packageInfo.requestedPermissionsFlags ?: return false
        return flags.getOrNull(permissionIndex)?.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
    }
}

data class PermissionSnapshot(
    val usageAccess: Boolean?,
    val notifications: Boolean?,
    val overlay: Boolean?,
    val writeSecureSettings: Boolean?
)

private const val PERMISSION_SNAPSHOT_LOG_TAG = "PermissionSnapshot"

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoWithPermissions(packageName: String): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    } catch (error: Exception) {
        Log.w(
            PERMISSION_SNAPSHOT_LOG_TAG,
            "component=PermissionSnapshotProvider operation=getPackageInfoWithPermissions package=$packageName failure=${error.javaClass.simpleName}",
            error
        )
        null
    }
}
