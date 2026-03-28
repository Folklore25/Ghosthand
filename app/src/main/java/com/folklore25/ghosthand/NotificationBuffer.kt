package com.folklore25.ghosthand

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
