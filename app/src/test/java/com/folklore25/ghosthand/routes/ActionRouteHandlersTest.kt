/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.NodeBounds
import com.folklore25.ghosthand.FlatAccessibilityNode
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
