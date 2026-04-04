/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.observation

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

import java.time.Instant
import java.util.ArrayDeque

data class GhosthandObservationEvent(
    val cursor: Long,
    val type: String,
    val timestamp: String,
    val packageName: String? = null,
    val activity: String? = null,
    val route: String? = null,
    val evidence: Map<String, Any?> = emptyMap()
)

data class GhosthandObservationBatch(
    val requestedSinceCursor: Long,
    val latestCursor: Long,
    val nextCursor: Long,
    val oldestCursor: Long,
    val droppedBeforeCursor: Boolean,
    val retentionLimit: Int,
    val events: List<GhosthandObservationEvent>
)

class GhosthandObservationLog(
    private val retentionLimit: Int = DEFAULT_RETENTION_LIMIT,
    private val nowProvider: () -> Instant = { Instant.now() }
) {
    private val events = ArrayDeque<GhosthandObservationEvent>()
    private var nextCursor = 1L

    @Synchronized
    fun append(
        type: String,
        packageName: String? = null,
        activity: String? = null,
        route: String? = null,
        evidence: Map<String, Any?> = emptyMap()
    ): GhosthandObservationEvent {
        val event = GhosthandObservationEvent(
            cursor = nextCursor++,
            type = type,
            timestamp = nowProvider().toString(),
            packageName = packageName,
            activity = activity,
            route = route,
            evidence = evidence.filterValues { it != null }
        )
        events.addLast(event)
        while (events.size > retentionLimit) {
            events.removeFirst()
        }
        return event
    }

    @Synchronized
    fun readSince(
        sinceCursor: Long? = null,
        limit: Int = DEFAULT_PAGE_LIMIT
    ): GhosthandObservationBatch {
        val normalizedSince = sinceCursor ?: 0L
        val boundedLimit = limit.coerceIn(1, MAX_PAGE_LIMIT)
        val snapshot = events.toList()
        val oldestCursor = snapshot.firstOrNull()?.cursor ?: 0L
        val latestCursor = snapshot.lastOrNull()?.cursor ?: 0L
        val droppedBeforeCursor = oldestCursor > 0 && normalizedSince < (oldestCursor - 1)
        val batchEvents = snapshot
            .asSequence()
            .filter { it.cursor > normalizedSince }
            .take(boundedLimit)
            .toList()

        return GhosthandObservationBatch(
            requestedSinceCursor = normalizedSince,
            latestCursor = latestCursor,
            nextCursor = batchEvents.lastOrNull()?.cursor ?: latestCursor,
            oldestCursor = oldestCursor,
            droppedBeforeCursor = droppedBeforeCursor,
            retentionLimit = retentionLimit,
            events = batchEvents
        )
    }

    companion object {
        const val DEFAULT_RETENTION_LIMIT = 128
        const val DEFAULT_PAGE_LIMIT = 50
        const val MAX_PAGE_LIMIT = 100
    }
}
