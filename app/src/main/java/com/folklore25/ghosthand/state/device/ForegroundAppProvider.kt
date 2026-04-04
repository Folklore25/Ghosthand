/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.device

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
