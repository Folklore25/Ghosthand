/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.observation

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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventLogTest {
    @Test
    fun readSinceReturnsOrderedEventsAndTracksRetentionBoundaries() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z"),
            Instant.parse("2026-04-04T00:00:02Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 2,
            nowProvider = { timestamps.next() }
        )

        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.two", activity = "ExampleActivity")
        log.append(type = "screen_readability_changed", packageName = "pkg.two")

        val batch = log.readSince(sinceCursor = 0)

        assertEquals(2, batch.events.size)
        assertEquals(2L, batch.oldestCursor)
        assertEquals(3L, batch.latestCursor)
        assertEquals(3L, batch.nextCursor)
        assertTrue(batch.droppedBeforeCursor)
        assertEquals(listOf(2L, 3L), batch.events.map { it.cursor })
        assertEquals(listOf("window_changed", "screen_readability_changed"), batch.events.map { it.type })
    }

    @Test
    fun readSinceFiltersEventsAfterRequestedCursor() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z"),
            Instant.parse("2026-04-04T00:00:02Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 8,
            nowProvider = { timestamps.next() }
        )

        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.two")
        log.append(type = "preview_became_available", packageName = "pkg.two")

        val batch = log.readSince(sinceCursor = 1)

        assertFalse(batch.droppedBeforeCursor)
        assertEquals(listOf(2L, 3L), batch.events.map { it.cursor })
        assertEquals(1L, batch.requestedSinceCursor)
    }
}
