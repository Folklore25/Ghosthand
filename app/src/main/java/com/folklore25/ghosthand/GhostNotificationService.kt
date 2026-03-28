package com.folklore25.ghosthand

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * NotificationListenerService stub for Ghosthand.
 *
 * This service enables notification interception when the user grants notification
 * listener access in system settings. Without this access, Ghosthand can still
 * POST and CANCEL notifications using NotificationManager (see NotificationDispatcher).
 *
 * To enable: Settings → Apps → Ghosthand → Notification access → enable.
 *
 * Currently this is a minimal stub. Future work may extend this to expose
 * intercepted notifications via the /notify GET endpoint.
 */
class GhostNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification
        val extras = notification?.extras
        NotificationBuffer.recordPosted(
            BufferedNotification(
                packageName = sbn?.packageName,
                title = extras?.getCharSequence("android.title")?.toString(),
                text = extras?.getCharSequence("android.text")?.toString(),
                tag = sbn?.tag,
                id = sbn?.id ?: -1
            )
        )
        Log.d(LOG_TAG, "event=notification_posted package=${sbn?.packageName} tag=${sbn?.tag}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        NotificationBuffer.recordRemoved(
            packageName = sbn?.packageName,
            tag = sbn?.tag,
            id = sbn?.id ?: -1
        )
        Log.d(LOG_TAG, "event=notification_removed package=${sbn?.packageName} tag=${sbn?.tag}")
    }

    private companion object {
        const val LOG_TAG = "GhostNotification"
    }
}
