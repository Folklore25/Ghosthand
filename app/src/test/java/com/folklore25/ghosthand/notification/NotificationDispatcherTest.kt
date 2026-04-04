/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.notification

import android.app.Notification
import android.app.NotificationChannel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDispatcherTest {
    @After
    fun tearDown() {
        NotificationBuffer.clearForTesting()
    }

    @Test
    fun cancelNotificationUsesRecordedTagAndRemovesMatchingEntry() {
        NotificationBuffer.recordPosted(
            BufferedNotification(
                packageName = TEST_PACKAGE,
                title = "Ghosthand",
                text = "Hello",
                tag = "custom_tag",
                id = 42
            )
        )
        val gateway = FakeNotificationManagerGateway()

        val result = cancelTrackedNotification(TEST_PACKAGE, 42, gateway)

        assertTrue(result.performed)
        assertEquals("notification_canceled", result.attemptedPath)
        assertEquals(listOf(CanceledNotification("custom_tag", 42)), gateway.canceledNotifications)
        assertTrue(NotificationBuffer.snapshotForTesting().isEmpty())
    }

    @Test
    fun cancelNotificationFailsWhenNoRecordedIdentityExists() {
        val gateway = FakeNotificationManagerGateway()

        val result = cancelTrackedNotification(TEST_PACKAGE, 404, gateway)

        assertFalse(result.performed)
        assertEquals("notification_not_found", result.attemptedPath)
        assertTrue(gateway.canceledNotifications.isEmpty())
    }

    @Test
    fun recordRemovedLeavesBufferUntouchedWhenTagDoesNotMatch() {
        NotificationBuffer.recordPosted(
            BufferedNotification(
                packageName = TEST_PACKAGE,
                title = "Ghosthand",
                text = "Hello",
                tag = "custom_tag",
                id = 7
            )
        )

        val removed = NotificationBuffer.recordRemoved(
            packageName = TEST_PACKAGE,
            tag = "ghosthand_notify",
            id = 7
        )

        assertFalse(removed)
        val notifications = NotificationBuffer.snapshotForTesting()
        assertEquals(1, notifications.size)
        assertEquals("custom_tag", notifications.first().tag)
    }

    private class FakeNotificationManagerGateway : NotificationManagerGateway {
        val canceledNotifications = mutableListOf<CanceledNotification>()

        override fun createNotificationChannel(channel: NotificationChannel) = Unit

        override fun notify(tag: String?, notificationId: Int, notification: Notification) = Unit

        override fun cancel(tag: String?, notificationId: Int) {
            canceledNotifications += CanceledNotification(tag, notificationId)
        }
    }

    private data class CanceledNotification(val tag: String?, val id: Int)

    private companion object {
        const val TEST_PACKAGE = "com.folklore25.ghosthand"
    }
}
