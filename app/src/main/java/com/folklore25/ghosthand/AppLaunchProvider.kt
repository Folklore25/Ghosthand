/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager

internal data class AppLaunchResult(
    val launched: Boolean,
    val packageName: String,
    val label: String?,
    val strategy: String = "package_launch_intent",
    val reason: String,
    val error: String? = null
)

internal class AppLaunchProvider internal constructor(
    private val isInstalled: (String) -> Boolean,
    private val resolveLabel: (String) -> String?,
    private val resolveLaunchIntent: (String) -> Intent?,
    private val resolveLauncherActivityIntent: (String) -> Intent?,
    private val startActivity: (Intent) -> Unit
) {
    constructor(context: Context) : this(
        isInstalled = { packageName ->
            try {
                context.packageManager.getApplicationInfoCompat(packageName)
                true
            } catch (_: Exception) {
                false
            }
        },
        resolveLabel = { packageName ->
            try {
                val applicationInfo = context.packageManager.getApplicationInfoCompat(packageName)
                context.packageManager.getApplicationLabel(applicationInfo)?.toString()
            } catch (_: Exception) {
                null
            }
        },
        resolveLaunchIntent = { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName)
        },
        resolveLauncherActivityIntent = { packageName ->
            context.packageManager.queryLauncherActivityIntent(packageName)
        },
        startActivity = { intent ->
            context.startActivity(
                Intent(intent).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            )
        }
    )

    fun launch(packageName: String): AppLaunchResult {
        if (!isInstalled(packageName)) {
            return AppLaunchResult(
                launched = false,
                packageName = packageName,
                label = null,
                reason = "package_not_installed"
            )
        }

        val label = resolveLabel(packageName)
        val primaryLaunchIntent = resolveLaunchIntent(packageName)
        val fallbackLaunchIntent = if (primaryLaunchIntent == null) {
            resolveLauncherActivityIntent(packageName)
        } else {
            null
        }
        val launchIntent = primaryLaunchIntent ?: fallbackLaunchIntent ?: return AppLaunchResult(
            launched = false,
            packageName = packageName,
            label = label,
            reason = "launch_intent_unavailable"
        )
        val strategy = if (primaryLaunchIntent != null) {
            "package_launch_intent"
        } else {
            "launcher_activity_query"
        }

        return try {
            startActivity(launchIntent)
            AppLaunchResult(
                launched = true,
                packageName = packageName,
                label = label,
                strategy = strategy,
                reason = "launched"
            )
        } catch (error: ActivityNotFoundException) {
            AppLaunchResult(
                launched = false,
                packageName = packageName,
                label = label,
                strategy = strategy,
                reason = "launch_attempt_failed",
                error = error.javaClass.simpleName
            )
        } catch (error: Exception) {
            AppLaunchResult(
                launched = false,
                packageName = packageName,
                label = label,
                strategy = strategy,
                reason = "launch_attempt_failed",
                error = error.javaClass.simpleName
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.queryLauncherActivityIntent(packageName: String): Intent? {
    val queryIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setPackage(packageName)
    }
    val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(queryIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        queryIntentActivities(queryIntent, 0)
    }.firstOrNull() ?: return null

    return Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(
            resolved.activityInfo.packageName,
            resolved.activityInfo.name
        )
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        getApplicationInfo(packageName, 0)
    }
