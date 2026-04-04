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

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.ArrayDeque

object NotificationBuffer {
    private val entries = ArrayDeque<BufferedNotification>()

    @Synchronized
    fun recordPosted(entry: BufferedNotification) {
        entries.addFirst(entry)
        trim()
    }

    @Synchronized
    fun recordRemoved(packageName: String?, tag: String?, id: Int) {
        entries.removeAll { existing ->
            existing.packageName == packageName &&
                existing.tag == tag &&
                existing.id == id
        }
    }

    @Synchronized
    fun toJson(packageFilter: String?, excludedPackages: Set<String>): JSONObject {
        val items = JSONArray()
        entries
            .asSequence()
            .filter { packageFilter.isNullOrBlank() || it.packageName == packageFilter }
            .filter { it.packageName !in excludedPackages }
            .forEach { entry ->
                items.put(
                    JSONObject()
                        .put("package", entry.packageName ?: JSONObject.NULL)
                        .put("title", entry.title ?: JSONObject.NULL)
                        .put("text", entry.text ?: JSONObject.NULL)
                        .put("tag", entry.tag ?: JSONObject.NULL)
                        .put("id", entry.id)
                        .put("postedAt", entry.postedAt)
                )
            }

        return JSONObject().put("notifications", items)
    }

    private fun trim() {
        while (entries.size > MAX_NOTIFICATIONS) {
            entries.removeLast()
        }
    }

    private const val MAX_NOTIFICATIONS = 50
}

data class BufferedNotification(
    val packageName: String?,
    val title: String?,
    val text: String?,
    val tag: String?,
    val id: Int,
    val postedAt: String = Instant.now().toString()
)
