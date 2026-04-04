/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.runtime

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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.folklore25.ghosthand.server.LocalApiServer

class GhosthandForegroundService : Service() {

    private val localApiServer by lazy {
        LocalApiServer(
            context = this,
            runtimeStateProvider = RuntimeStateStore::snapshot
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            GhosthandServiceRegistry.register(this)
            createNotificationChannel()
            RuntimeStateStore.refreshHomeDiagnostics(this)
            RuntimeStateStore.refreshAccessibilityStatus(this)
            RuntimeStateStore.markServiceCreated()
            localApiServer.start()
            Log.i(LOG_TAG, "Foreground service created")
        } catch (error: Exception) {
            RuntimeStateStore.recordServiceBootstrapFailure(
                stage = "service_onCreate",
                preconditions = "serviceRegistered=${GhosthandServiceRegistry.getInstanceIfRunning() != null}",
                error = error
            )
            Log.e(LOG_TAG, "Foreground service bootstrap failed in onCreate", error)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            RuntimeStateStore.refreshHomeDiagnostics(this)
            RuntimeStateStore.refreshAccessibilityStatus(this)
            RuntimeStateStore.markServiceRunning()
            Log.i(LOG_TAG, "Foreground service started")
            START_STICKY
        } catch (error: Exception) {
            RuntimeStateStore.recordServiceBootstrapFailure(
                stage = "service_onStartCommand",
                preconditions = "sdk=${Build.VERSION.SDK_INT} startId=$startId",
                error = error
            )
            Log.e(LOG_TAG, "Foreground service failed to enter running state", error)
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        try {
            localApiServer.stop()
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Failed to stop LocalApiServer during service teardown", error)
        } finally {
            GhosthandServiceRegistry.unregister()
            RuntimeStateStore.markServiceStopped()
            Log.i(LOG_TAG, "Foreground service destroyed")
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        localApiServer.setMediaProjection(projection)
    }

    fun hasMediaProjection(): Boolean = localApiServer.hasMediaProjection()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_description)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "ghosthand_runtime"
        const val NOTIFICATION_ID = 1001
        const val LOG_TAG = "GhosthandService"
    }
}

/**
 * Separate object to hold the service singleton, avoiding companion visibility issues.
 */
object GhosthandServiceRegistry {
    @Volatile
    private var currentInstance: GhosthandForegroundService? = null

    fun register(service: GhosthandForegroundService) {
        currentInstance = service
    }

    fun unregister() {
        currentInstance = null
    }

    fun getInstanceIfRunning(): GhosthandForegroundService? = currentInstance
}
