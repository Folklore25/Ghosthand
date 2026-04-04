/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.notification

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
