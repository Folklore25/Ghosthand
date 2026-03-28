package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandApiPayloadsTest {
    @Test
    fun screenPayloadIncludesActionReadyGeometryAndFilters() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Button", clickable = true, centerX = 10, centerY = 20),
                node("p0.1@tsnap", text = "Input", editable = true, centerX = 30, centerY = 40),
                node("p0.2@tsnap", text = "List", scrollable = true, centerX = 50, centerY = 60)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = "com.example",
            clickableOnly = true
        )

        val elements = payload["elements"] as List<*>
        assertEquals(1, elements.size)
        val button = elements.first() as Map<*, *>
        assertEquals("Button", button["text"])
        assertEquals(10, button["centerX"])
        assertEquals(20, button["centerY"])
        assertEquals("[0,0][100,100]", button["bounds"])
    }

    @Test
    fun rawTreePayloadBuildsNestedChildren() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", className = "Root"),
                node("p0.0@tsnap", text = "Child"),
                node("p0.0.0@tsnap", text = "Grandchild")
            )
        )

        val payload = GhosthandApiPayloads.rawTreeFields(snapshot)
        val root = payload["root"] as Map<*, *>
        assertEquals("p0@tsnap", root["nodeId"])
        val child = (root["children"] as List<*>).first() as Map<*, *>
        assertEquals("Child", child["text"])
        val grandchild = (child["children"] as List<*>).first() as Map<*, *>
        assertEquals("Grandchild", grandchild["text"])
    }

    @Test
    fun findPayloadIncludesMatchMetadataAndGeometry() {
        val match = node(
            nodeId = "p0.1@tsnap",
            text = "Target",
            resourceId = "com.example:id/target",
            clickable = true,
            editable = false,
            scrollable = false,
            centerX = 77,
            centerY = 88
        )
        val result = FindNodeResult(
            found = true,
            node = match,
            matches = listOf(match),
            selectedIndex = 0
        )

        val payload = GhosthandApiPayloads.findFields(result)
        assertTrue(payload["found"] as Boolean)
        assertEquals(1, payload["matchCount"])
        assertEquals("Target", payload["text"])
        assertEquals("com.example:id/target", payload["id"])
        assertEquals(77, payload["centerX"])
        assertEquals(88, payload["centerY"])
    }

    @Test
    fun findPayloadUsesNullNodeWhenNoMatchExists() {
        val payload = GhosthandApiPayloads.findFields(
            FindNodeResult(found = false, node = null, matches = emptyList(), selectedIndex = 0)
        )
        assertEquals(null, payload["node"])
        assertEquals(0, payload["matchCount"])
    }

    private fun snapshot(nodes: List<FlatAccessibilityNode>): AccessibilityTreeSnapshot {
        return AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = nodes
        )
    }

    private fun node(
        nodeId: String,
        text: String? = null,
        resourceId: String? = null,
        className: String = "android.widget.TextView",
        clickable: Boolean = false,
        editable: Boolean = false,
        scrollable: Boolean = false,
        centerX: Int = 0,
        centerY: Int = 0
    ): FlatAccessibilityNode {
        return FlatAccessibilityNode(
            nodeId = nodeId,
            text = text,
            contentDesc = null,
            resourceId = resourceId,
            className = className,
            clickable = clickable,
            editable = editable,
            enabled = true,
            focused = false,
            scrollable = scrollable,
            centerX = centerX,
            centerY = centerY,
            bounds = NodeBounds(0, 0, 100, 100)
        )
    }
}
