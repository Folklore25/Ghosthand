package com.folklore25.ghosthand

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

class GhosthandForegroundService : Service() {

    private val localApiServer by lazy {
        LocalApiServer(
            context = this,
            runtimeStateProvider = RuntimeStateStore::snapshot
        )
    }

    override fun onCreate() {
        super.onCreate()
        GhosthandServiceRegistry.register(this)
        createNotificationChannel()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        RuntimeStateStore.markServiceCreated()
        localApiServer.start()
        Log.i(LOG_TAG, "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        RuntimeStateStore.markServiceRunning()
        Log.i(LOG_TAG, "Foreground service started")
        return START_STICKY
    }

    override fun onDestroy() {
        localApiServer.stop()
        GhosthandServiceRegistry.unregister()
        RuntimeStateStore.markServiceStopped()
        Log.i(LOG_TAG, "Foreground service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        localApiServer.setMediaProjection(projection)
    }

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
