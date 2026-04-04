/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.ClickAttemptResult
import com.folklore25.ghosthand.FlatAccessibilityNode
import com.folklore25.ghosthand.NodeBounds
import com.folklore25.ghosthand.state.InputKeyDispatchResult
import com.folklore25.ghosthand.state.InputOperationResult
import com.folklore25.ghosthand.state.InputTextMutationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandInteractionPayloadsTest {
    @Test
    fun clickFieldsCarryNormalizedEvidenceAndObservationShiftHints() {
        val fields = GhosthandInteractionPayloads.clickFields(
            result = ClickAttemptResult.success(
                attemptedPath = "node_click",
                effect = ActionEffectObservation(
                    stateChanged = true,
                    beforeSnapshotToken = "before",
                    afterSnapshotToken = "after",
                    finalPackageName = "com.example",
                    finalActivity = "ExampleActivity"
                )
            ),
            fallbackSnapshot = weakAccessibilitySnapshot()
        )

        assertEquals(true, fields["performed"])
        assertEquals("node_click", fields["attemptedPath"])
        assertEquals("accessibility", fields["backendUsed"])
        assertEquals(true, fields["stateChanged"])
        assertEquals("before", fields["beforeSnapshotToken"])
        assertEquals("after", fields["afterSnapshotToken"])
        assertEquals("hybrid", fields["suggestedSource"])
        assertEquals("accessibility_operationally_insufficient", fields["fallbackReason"])
        val postActionState = fields["postActionState"] as Map<*, *>
        assertEquals("hybrid", postActionState["suggestedSource"])
    }

    @Test
    fun inputFieldsProjectSharedEvidenceFamilyBesideOperationBreakdown() {
        val fields = GhosthandInputPayloads.inputResultFields(
            result = InputOperationResult(
                performed = true,
                textMutation = InputTextMutationResult(
                    requested = true,
                    performed = true,
                    action = "set",
                    previousText = "",
                    finalText = "wifi",
                    backendUsed = "accessibility",
                    failureReason = null,
                    attemptedPath = "set_text"
                ),
                keyDispatch = InputKeyDispatchResult(
                    requested = true,
                    performed = true,
                    key = "enter",
                    backendUsed = "accessibility",
                    failureReason = null,
                    attemptedPath = "input_key"
                ),
                postActionState = PostActionState(
                    packageName = "com.example",
                    activity = "SearchActivity",
                    snapshotToken = "after",
                    suggestedSource = "hybrid",
                    fallbackReason = "accessibility_operationally_insufficient"
                )
            ),
            actionEffect = ActionEffectObservation(
                stateChanged = true,
                beforeSnapshotToken = "before",
                afterSnapshotToken = "after",
                finalPackageName = "com.example",
                finalActivity = "SearchActivity"
            ),
            attemptedPath = "composite_input",
            backendUsed = "accessibility"
        )

        assertEquals(true, fields["performed"])
        assertEquals("composite_input", fields["attemptedPath"])
        assertEquals("accessibility", fields["backendUsed"])
        assertEquals(true, fields["stateChanged"])
        assertEquals(true, fields["textChanged"])
        assertEquals(true, fields["keyDispatched"])
        assertEquals("hybrid", fields["suggestedSource"])
        assertEquals("accessibility_operationally_insufficient", fields["fallbackReason"])
        assertTrue(fields.containsKey("postActionState"))
        assertTrue(fields.containsKey("textMutation"))
        assertTrue(fields.containsKey("keyDispatch"))
    }

    private fun weakAccessibilitySnapshot(): AccessibilityTreeSnapshot {
        return AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = "ExampleActivity",
            snapshotToken = "after",
            capturedAt = "2026-04-04T00:00:00Z",
            nodes = listOf(
                FlatAccessibilityNode(
                    nodeId = "n0",
                    text = "",
                    contentDesc = "",
                    resourceId = "",
                    className = "android.widget.FrameLayout",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 10,
                    centerY = 10,
                    bounds = NodeBounds(0, 0, 20, 20)
                ),
                FlatAccessibilityNode(
                    nodeId = "n1",
                    text = "Only node",
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.TextView",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 30,
                    centerY = 30,
                    bounds = NodeBounds(20, 20, 40, 40)
                ),
                FlatAccessibilityNode(
                    nodeId = "n2",
                    text = "",
                    contentDesc = "",
                    resourceId = "",
                    className = "android.widget.FrameLayout",
                    clickable = false,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 50,
                    centerY = 50,
                    bounds = NodeBounds(40, 40, 60, 60)
                )
            ),
            foregroundStableDuringCapture = true
        )
    }
}
