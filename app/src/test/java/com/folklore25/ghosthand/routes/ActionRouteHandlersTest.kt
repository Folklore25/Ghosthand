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

import com.folklore25.ghosthand.routes.action.buildActionEffectDisclosure
import com.folklore25.ghosthand.routes.action.observeScrollSurfaceChange
import com.folklore25.ghosthand.routes.action.toActionEffectObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionRouteHandlersTest {
    @Test
    fun surfaceObservationCarriesAfterSnapshotForEvidenceProjection() {
        val before = snapshot("before", "com.example", "FirstActivity")
        val after = snapshot("after", "com.example", "SecondActivity")

        val observation = observeScrollSurfaceChange(before, after)

        assertTrue(observation.surfaceChanged)
        assertEquals("after", observation.afterSnapshotToken)
        assertEquals(after, observation.afterSnapshot)
        assertEquals(true, observation.toActionEffectObservation().stateChanged)
    }

    @Test
    fun ambiguityDisclosureOnlyAppearsWhenActionDispatchedWithoutObservedChange() {
        assertNotNull(buildActionEffectDisclosure("/tap", true, false))
        assertNull(buildActionEffectDisclosure("/tap", true, true))
        assertNull(buildActionEffectDisclosure("/tap", false, false))
    }

    private fun snapshot(token: String, packageName: String, activity: String): AccessibilityTreeSnapshot {
        return AccessibilityTreeSnapshot(
            packageName = packageName,
            activity = activity,
            snapshotToken = token,
            capturedAt = "2026-04-04T00:00:00Z",
            nodes = listOf(
                FlatAccessibilityNode(
                    nodeId = "n0",
                    text = "Node",
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.TextView",
                    clickable = true,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 10,
                    centerY = 10,
                    bounds = NodeBounds(0, 0, 20, 20)
                )
            ),
            foregroundStableDuringCapture = true
        )
    }
}
