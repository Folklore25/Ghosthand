/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.notification

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

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Dispatches notification post/cancel via NotificationManager.
 * Uses NotificationManagerCompat for Android 13+ permission handling.
 *
 * Does NOT require NotificationListenerService for POST/CANCEL — uses
 * NotificationManager in Ghosthand's own app process.
 */
class NotificationDispatcher(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Ghosthand operator notifications"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Posts a notification with the given title and text.
     * Returns the notification tag+id so it can be canceled later.
     */
    fun postNotification(title: String, text: String, tag: String? = null): NotificationPostResult {
        if (!canPostNotifications()) {
            return NotificationPostResult(
                performed = false,
                notificationId = null,
                attemptedPath = "permission_denied"
            )
        }

        val notificationId = nextNotificationId()
        val effectiveTag = tag ?: TAG_PREFIX

        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .build()

        return try {
            notificationManager.notify(effectiveTag, notificationId, notification)
            NotificationBuffer.recordPosted(
                BufferedNotification(
                    packageName = context.packageName,
                    title = title.ifBlank { context.getString(R.string.app_name) },
                    text = text,
                    tag = effectiveTag,
                    id = notificationId
                )
            )
            NotificationPostResult(
                performed = true,
                notificationId = notificationId,
                attemptedPath = "notification_posted"
            )
        } catch (e: Exception) {
            NotificationPostResult(
                performed = false,
                notificationId = null,
                attemptedPath = "notification_post_failed_${e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Cancels the notification with the given ID.
     */
    fun cancelNotification(notificationId: Int): NotificationCancelResult {
        return try {
            notificationManager.cancel(notificationId)
            NotificationBuffer.recordRemoved(
                packageName = context.packageName,
                tag = TAG_PREFIX,
                id = notificationId
            )
            NotificationCancelResult(
                performed = true,
                attemptedPath = "notification_canceled"
            )
        } catch (e: Exception) {
            NotificationCancelResult(
                performed = false,
                attemptedPath = "notification_cancel_failed_${e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Returns true if notification permission is granted.
     */
    fun hasPermission(): Boolean = canPostNotifications()

    @Synchronized
    private fun nextNotificationId(): Int {
        return ++lastNotificationId
    }

    private companion object {
        const val CHANNEL_ID = "ghosthand_notifications_high"
        const val CHANNEL_NAME = "Ghosthand"
        const val TAG_PREFIX = "ghosthand_notify"
        @Volatile
        private var lastNotificationId = 1000
    }
}

data class NotificationPostResult(
    val performed: Boolean,
    val notificationId: Int?,
    val attemptedPath: String
)

data class NotificationCancelResult(
    val performed: Boolean,
    val attemptedPath: String
)
