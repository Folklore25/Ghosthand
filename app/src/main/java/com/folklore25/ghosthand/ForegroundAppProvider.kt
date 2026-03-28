/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import org.json.JSONObject
import java.time.Instant

class ForegroundAppProvider(
    private val context: Context
) {
    private val permissionSnapshotProvider = PermissionSnapshotProvider(context)

    fun snapshot(): ForegroundAppSnapshot {
        if (permissionSnapshotProvider.snapshot().usageAccess != true) {
            return ForegroundAppSnapshot()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val event = UsageEvents.Event()
        val events = usageStatsManager.queryEvents(now - LOOKBACK_WINDOW_MS, now)

        var latestPackageName: String? = null
        var latestActivity: String? = null
        var latestTimestamp: Long? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                latestPackageName = event.packageName
                latestActivity = event.className
                latestTimestamp = event.timeStamp
            }
        }

        val packageName = latestPackageName ?: return ForegroundAppSnapshot()
        val label = resolveLabel(packageName)

        return ForegroundAppSnapshot(
            packageName = packageName,
            activity = latestActivity,
            label = label,
            timestamp = latestTimestamp?.let { Instant.ofEpochMilli(it).toString() }
        )
    }

    fun toJson(snapshot: ForegroundAppSnapshot): JSONObject {
        return JSONObject()
            .put("packageName", snapshot.packageName ?: JSONObject.NULL)
            .put("activity", snapshot.activity ?: JSONObject.NULL)
            .put("label", snapshot.label ?: JSONObject.NULL)
            .put("timestamp", snapshot.timestamp ?: JSONObject.NULL)
    }

    private fun resolveLabel(packageName: String): String? {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo)?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val LOOKBACK_WINDOW_MS = 10 * 60 * 1000L
    }
}

data class ForegroundAppSnapshot(
    val packageName: String? = null,
    val activity: String? = null,
    val label: String? = null,
    val timestamp: String? = null
)
