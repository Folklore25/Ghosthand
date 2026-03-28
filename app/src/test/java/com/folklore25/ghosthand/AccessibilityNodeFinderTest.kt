package com.folklore25.ghosthand

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
    }

    private fun node(
        nodeId: String,
        text: String? = null,
        clickable: Boolean = false
    ): FlatAccessibilityNode {
        return FlatAccessibilityNode(
            nodeId = nodeId,
            text = text,
            contentDesc = null,
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
