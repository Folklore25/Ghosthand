/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityNodeFinderTest {
    private val finder = AccessibilityNodeFinder()

    @Test
    fun clickableOnlyResolvesToClickableAncestor() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                node("p0.0.0@tsnap", text = "Target", clickable = false)
            )
        )

        val result = finder.findNodes(
            snapshot = snapshot,
            strategy = "text",
            query = "Target",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
        assertEquals(1, result.matches.size)
    }

    @Test
    fun indexSelectsNthMatch() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Row"),
                node("p0.1@tsnap", text = "Row")
            )
        )

        val result = finder.findNodes(snapshot, "text", "Row", clickableOnly = false, index = 1)

        assertTrue(result.found)
        assertEquals("p0.1@tsnap", result.node?.nodeId)
        assertEquals(2, result.matches.size)
    }

    @Test
    fun nonZeroIndexPreservesSelectedMatchBoundsWhenLaterMatchIsValid() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap"),
                FlatAccessibilityNode(
                    nodeId = "p0.0@tsnap",
                    text = "Row",
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.TextView",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 0,
                    centerY = 0,
                    bounds = NodeBounds(0, 0, 0, 0)
                ),
                FlatAccessibilityNode(
                    nodeId = "p0.1@tsnap",
                    text = "Row",
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.TextView",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 25,
                    centerY = 35,
                    bounds = NodeBounds(10, 20, 40, 50)
                )
            )
        )

        val result = finder.findNodes(snapshot, "text", "Row", clickableOnly = false, index = 1)

        assertTrue(result.found)
        assertEquals("p0.1@tsnap", result.node?.nodeId)
        assertEquals(25, result.node?.centerX)
        assertEquals(35, result.node?.centerY)
        assertEquals(10, result.node?.bounds?.left)
        assertEquals(50, result.node?.bounds?.bottom)
    }

    @Test
    fun noClickableAncestorReturnsNotFoundForClickableOnly() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", text = "Target", clickable = false)
            )
        )

        val result = finder.findNodes(snapshot, "text", "Target", clickableOnly = true, index = 0)

        assertFalse(result.found)
        assertTrue(result.matches.isEmpty())
        assertEquals("actionable_target_not_found", result.missHint?.failureCategory)
        assertEquals(1, result.missHint?.selectorMatchCount)
        assertEquals(0, result.missHint?.actionableMatchCount)
    }

    @Test
    fun clickResolutionFallsBackToContainsAndClickableAncestor() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                node("p0.0.0@tsnap", text = "Vibe Coding... more", clickable = false)
            )
        )

        val result = finder.findNodesForClick(
            snapshot = snapshot,
            strategy = "text",
            query = "Vibe Coding",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
        assertNotNull(result.clickResolution)
        assertEquals("text", result.clickResolution?.requestedStrategy)
        assertEquals("textContains", result.clickResolution?.effectiveStrategy)
        assertEquals("text", result.clickResolution?.requestedSurface)
        assertEquals("text", result.clickResolution?.matchedSurface)
        assertEquals("exact", result.clickResolution?.requestedMatchSemantics)
        assertEquals("contains", result.clickResolution?.matchedMatchSemantics)
        assertEquals(false, result.clickResolution?.usedSurfaceFallback)
        assertEquals(true, result.clickResolution?.usedContainsFallback)
        assertEquals("p0.0.0@tsnap", result.clickResolution?.matchedNodeId)
        assertEquals(false, result.clickResolution?.matchedNodeClickable)
        assertEquals("p0.0@tsnap", result.clickResolution?.resolvedNodeId)
        assertEquals("clickable_ancestor", result.clickResolution?.resolutionKind)
        assertEquals(1, result.clickResolution?.ancestorDepth)
    }

    @Test
    fun clickResolutionCanFallBackFromTextToContentDescription() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                FlatAccessibilityNode(
                    nodeId = "p0.0.0@tsnap",
                    text = null,
                    contentDesc = "Settings",
                    resourceId = null,
                    className = "android.widget.ImageView",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 0,
                    centerY = 0,
                    bounds = NodeBounds(0, 0, 10, 10)
                )
            )
        )

        val result = finder.findNodesForClick(
            snapshot = snapshot,
            strategy = "text",
            query = "Settings",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
        assertEquals("text", result.clickResolution?.requestedSurface)
        assertEquals("contentDesc", result.clickResolution?.matchedSurface)
        assertEquals(true, result.clickResolution?.usedSurfaceFallback)
        assertEquals(false, result.clickResolution?.usedContainsFallback)
        assertEquals("p0.0.0@tsnap", result.clickResolution?.matchedNodeId)
        assertEquals("clickable_ancestor", result.clickResolution?.resolutionKind)
    }

    @Test
    fun clickResolutionCanFallBackFromContentDescriptionToText() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                node("p0.0.0@tsnap", text = "Settings", clickable = false)
            )
        )

        val result = finder.findNodesForClick(
            snapshot = snapshot,
            strategy = "contentDesc",
            query = "Settings",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("contentDesc", result.clickResolution?.requestedSurface)
        assertEquals("text", result.clickResolution?.matchedSurface)
        assertEquals(true, result.clickResolution?.usedSurfaceFallback)
        assertEquals(false, result.clickResolution?.usedContainsFallback)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
    }

    @Test
    fun clickResolutionFallsBackFromTextToContentDescription() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                node("p0.0.0@tsnap", contentDesc = "Settings", clickable = false)
            )
        )

        val result = finder.findNodesForClick(
            snapshot = snapshot,
            strategy = "text",
            query = "Settings",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
        assertEquals("text", result.clickResolution?.requestedStrategy)
        assertEquals("contentDesc", result.clickResolution?.effectiveStrategy)
        assertEquals("text", result.clickResolution?.requestedSurface)
        assertEquals("contentDesc", result.clickResolution?.matchedSurface)
        assertEquals("exact", result.clickResolution?.requestedMatchSemantics)
        assertEquals("exact", result.clickResolution?.matchedMatchSemantics)
        assertEquals(true, result.clickResolution?.usedSurfaceFallback)
        assertEquals(false, result.clickResolution?.usedContainsFallback)
        assertEquals("p0.0.0@tsnap", result.clickResolution?.matchedNodeId)
    }

    @Test
    fun clickResolutionFallsBackFromContentDescriptionToText() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", clickable = true),
                node("p0.0.0@tsnap", text = "Profile", clickable = false)
            )
        )

        val result = finder.findNodesForClick(
            snapshot = snapshot,
            strategy = "contentDesc",
            query = "Profile",
            clickableOnly = true,
            index = 0
        )

        assertTrue(result.found)
        assertEquals("p0.0@tsnap", result.node?.nodeId)
        assertEquals("contentDesc", result.clickResolution?.requestedStrategy)
        assertEquals("text", result.clickResolution?.effectiveStrategy)
        assertEquals("contentDesc", result.clickResolution?.requestedSurface)
        assertEquals("text", result.clickResolution?.matchedSurface)
        assertEquals("exact", result.clickResolution?.requestedMatchSemantics)
        assertEquals("exact", result.clickResolution?.matchedMatchSemantics)
        assertEquals(true, result.clickResolution?.usedSurfaceFallback)
        assertEquals(false, result.clickResolution?.usedContainsFallback)
        assertEquals("p0.0.0@tsnap", result.clickResolution?.matchedNodeId)
    }

    @Test
    fun exactTextMissHintsThatVisiblePrefixMayNeedContains() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Vibe Coding... more", clickable = false)
            )
        )

        val result = finder.findNodes(
            snapshot = snapshot,
            strategy = "text",
            query = "Vibe Coding",
            clickableOnly = false,
            index = 0
        )

        assertFalse(result.found)
        assertEquals("same_surface_contains_match_available", result.missHint?.failureCategory)
        assertEquals("text", result.missHint?.searchedSurface)
        assertEquals("exact", result.missHint?.matchSemantics)
        assertEquals("visible_text_is_part_of_a_longer_text_block", result.missHint?.likelyMissReason)
        assertTrue(result.missHint?.suggestedAlternateStrategies?.contains("textContains") == true)
    }

    @Test
    fun exactTextMissHintsThatLabelMayLiveInContentDescription() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                FlatAccessibilityNode(
                    nodeId = "p0.0@tsnap",
                    text = null,
                    contentDesc = "Settings",
                    resourceId = null,
                    className = "android.widget.ImageView",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 0,
                    centerY = 0,
                    bounds = NodeBounds(0, 0, 10, 10)
                )
            )
        )

        val result = finder.findNodes(
            snapshot = snapshot,
            strategy = "text",
            query = "Settings",
            clickableOnly = false,
            index = 0
        )

        assertFalse(result.found)
        assertEquals("meaningful_label_may_live_in_content_description", result.missHint?.likelyMissReason)
        assertTrue(result.missHint?.suggestedAlternateSurfaces?.contains("contentDesc") == true)
        assertTrue(result.missHint?.suggestedAlternateStrategies?.contains("contentDescContains") == true)
    }

    @Test
    fun contentDescriptionContainsMissSeparatesContainsMismatchFromSurfaceFallback() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(
                node("p0@tsnap", clickable = false),
                node("p0.0@tsnap", text = "Settings and privacy", clickable = false)
            )
        )

        val result = finder.findNodes(
            snapshot = snapshot,
            strategy = "contentDesc",
            query = "Settings",
            clickableOnly = false,
            index = 0
        )

        assertFalse(result.found)
        assertEquals("contentDesc", result.missHint?.searchedSurface)
        assertEquals("exact", result.missHint?.matchSemantics)
        assertEquals("meaningful_label_may_live_in_text", result.missHint?.likelyMissReason)
        assertEquals(listOf("text"), result.missHint?.suggestedAlternateSurfaces)
        assertEquals(listOf("textContains"), result.missHint?.suggestedAlternateStrategies)
    }

    private fun node(
        nodeId: String,
        text: String? = null,
        contentDesc: String? = null,
        clickable: Boolean = false
    ): FlatAccessibilityNode {
        return FlatAccessibilityNode(
            nodeId = nodeId,
            text = text,
            contentDesc = contentDesc,
            resourceId = null,
            className = "android.widget.TextView",
            clickable = clickable,
            editable = false,
            enabled = true,
            focused = false,
            scrollable = false,
            centerX = 0,
            centerY = 0,
            bounds = NodeBounds(0, 0, 10, 10)
        )
    }
}
