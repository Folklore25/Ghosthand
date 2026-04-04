/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.interaction.clipboard.ClipboardProvider
import com.folklore25.ghosthand.interaction.clipboard.ClipboardReadResult
import com.folklore25.ghosthand.interaction.clipboard.ClipboardWriteResult
import com.folklore25.ghosthand.notification.NotificationBuffer
import com.folklore25.ghosthand.notification.NotificationCancelResult
import com.folklore25.ghosthand.notification.NotificationDispatcher
import com.folklore25.ghosthand.notification.NotificationPostResult
import org.json.JSONObject

internal class StatePeripheralCoordinator(
    private val clipboardProvider: ClipboardProvider,
    private val notificationDispatcher: NotificationDispatcher
) {
    fun readClipboard(): ClipboardReadResult = clipboardProvider.readClipboard()

    fun writeClipboard(text: String): ClipboardWriteResult = clipboardProvider.writeClipboard(text)

    fun postNotification(title: String, text: String): NotificationPostResult {
        return notificationDispatcher.postNotification(title, text)
    }

    fun cancelNotification(notificationId: Int): NotificationCancelResult {
        return notificationDispatcher.cancelNotification(notificationId)
    }

    fun readNotifications(packageFilter: String?, excludedPackages: Set<String>): JSONObject {
        return NotificationBuffer.toJson(packageFilter, excludedPackages)
    }
}
