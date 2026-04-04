/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.observation.GhosthandObservationLog
import com.folklore25.ghosthand.routes.observation.ObservationRouteHandlers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventsRouteHandlersTest {
    @Test
    fun eventsRouteReturnsCursorFilteredBatch() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 10,
            nowProvider = { timestamps.next() }
        )
        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.one", activity = "MainActivity")

        val result = ObservationRouteHandlers(log).queryEvents(
            mapOf("since" to "1", "limit" to "10")
        )
        val batch = result.batch!!

        assertEquals(1, batch.events.size)
        assertEquals("window_changed", batch.events[0].type)
        assertEquals(2L, batch.latestCursor)
        assertEquals(2L, batch.nextCursor)
        assertFalse(batch.droppedBeforeCursor)
    }

    @Test
    fun eventsRouteRejectsMalformedCursorParameters() {
        val result = ObservationRouteHandlers(GhosthandObservationLog()).queryEvents(
            mapOf("since" to "oops")
        )

        assertNull(result.batch)
        assertTrue(result.errorMessage!!.contains("since"))
    }
}
