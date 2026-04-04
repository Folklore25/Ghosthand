/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

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
