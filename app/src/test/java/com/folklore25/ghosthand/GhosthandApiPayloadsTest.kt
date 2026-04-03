/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandApiPayloadsTest {
    @Test
    fun requestAndScreenHelpersShareTheExistingPublicContract() {
        val parsed = GhosthandInputPayloads.parseRequest(mapOf("text" to "hello world"))
        assertEquals(GhosthandApiPayloads.parseInputRequest(mapOf("text" to "hello world")), parsed)

        val payload = GhosthandApiPayloads.accessibilityScreenRead(
            snapshot = snapshot(
                nodes = listOf(
                    node("p0@tsnap"),
                    node("p0.0@tsnap", text = "Editor", editable = true, centerX = 30, centerY = 40)
                )
            ),
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        assertEquals(
            GhosthandApiPayloads.screenSummaryFields(payload),
            GhosthandScreenPayloads.summaryFields(payload)
        )
    }

    @Test
    fun parseInputRequestDefaultsPlainTextToSetAction() {
        val parsed = GhosthandApiPayloads.parseInputRequest(mapOf("text" to "hello world"))

        assertNull(parsed.errorMessage)
        assertNotNull(parsed.request)
        assertEquals(InputTextAction.SET, parsed.request!!.textAction)
        assertEquals("hello world", parsed.request!!.text)
        assertEquals(null, parsed.request!!.key)
    }

    @Test
    fun parseInputRequestRejectsShortcutLikeKeys() {
        val parsed = GhosthandApiPayloads.parseInputRequest(mapOf("key" to "ctrl+enter"))

        assertEquals("key must be one of: enter.", parsed.errorMessage)
        assertEquals(null, parsed.request)
    }

    @Test
    fun inputResultFieldsExposeSeparateTextAndKeyTruth() {
        val fields = GhosthandApiPayloads.inputResultFields(
            InputOperationResult(
                performed = false,
                textMutation = InputTextMutationResult(
                    requested = true,
                    performed = true,
                    action = "set",
                    previousText = "old",
                    finalText = "new",
                    backendUsed = "accessibility",
                    failureReason = null,
                    attemptedPath = "focused_set_text"
                ),
                keyDispatch = InputKeyDispatchResult(
                    requested = true,
                    performed = false,
                    key = "enter",
                    backendUsed = null,
                    failureReason = InputKeyFailureReason.ACTION_FAILED,
                    attemptedPath = "focused_ime_enter"
                )
            )
        )

        assertEquals(false, fields["performed"])
        assertEquals(true, fields["textChanged"])
        assertEquals(false, fields["keyDispatched"])
        val textMutation = fields["textMutation"] as Map<*, *>
        val keyDispatch = fields["keyDispatch"] as Map<*, *>
        assertEquals("set", textMutation["action"])
        assertEquals("new", textMutation["text"])
        assertEquals("enter", keyDispatch["key"])
        assertEquals("ACTION_FAILED", keyDispatch["failureReason"])
    }

    @Test
    fun clickPayloadIncludesObservedEffectFields() {
        val fields = GhosthandApiPayloads.actionEffectFields(
            ActionEffectObservation(
                stateChanged = false,
                beforeSnapshotToken = "snap-before",
                afterSnapshotToken = "snap-after",
                finalPackageName = "com.example.target",
                finalActivity = "TargetActivity"
            )
        )

        assertEquals(false, fields["stateChanged"])
        assertEquals("snap-before", fields["beforeSnapshotToken"])
        assertEquals("snap-after", fields["afterSnapshotToken"])
        assertEquals("com.example.target", fields["finalPackageName"])
        assertEquals("TargetActivity", fields["finalActivity"])
    }

    @Test
    fun clickFieldsIncludeObservedEffectFields() {
        val fields = GhosthandApiPayloads.clickFields(
            ClickAttemptResult.success(
                attemptedPath = "node_click",
                effect = ActionEffectObservation(
                    stateChanged = false,
                    beforeSnapshotToken = "snap-before",
                    afterSnapshotToken = "snap-after",
                    finalPackageName = "com.example.target",
                    finalActivity = "TargetActivity"
                )
            )
        )

        assertEquals(true, fields["performed"])
        assertEquals(false, fields["stateChanged"])
        assertEquals("snap-before", fields["beforeSnapshotToken"])
        assertEquals("snap-after", fields["afterSnapshotToken"])
        assertEquals("com.example.target", fields["finalPackageName"])
        assertEquals("TargetActivity", fields["finalActivity"])
    }

    @Test
    fun globalActionFieldsExposeDispatchAndObservedStateSeparately() {
        val fields = GhosthandApiPayloads.globalActionFields(
            GlobalActionResult(
                performed = true,
                attemptedPath = "global_action",
                effect = ActionEffectObservation(
                    stateChanged = true,
                    beforeSnapshotToken = "before",
                    afterSnapshotToken = "after",
                    finalPackageName = "com.android.launcher",
                    finalActivity = "Launcher"
                )
            )
        )

        assertEquals(true, fields["performed"])
        assertEquals(true, fields["stateChanged"])
        assertEquals("before", fields["beforeSnapshotToken"])
        assertEquals("after", fields["afterSnapshotToken"])
        assertEquals("com.android.launcher", fields["finalPackageName"])
        assertEquals("Launcher", fields["finalActivity"])
    }

    @Test
    fun postActionStateFieldsPreferCompactObservedSubset() {
        val fields = GhosthandApiPayloads.postActionStateFields(
            PostActionState(
                packageName = "com.example.target",
                activity = "TargetActivity",
                snapshotToken = "snap-after",
                renderMode = "accessibility",
                surfaceReadability = "good",
                visualAvailable = true
            )
        )

        assertEquals("com.example.target", fields["packageName"])
        assertEquals("TargetActivity", fields["activity"])
        assertEquals("snap-after", fields["snapshotToken"])
        assertEquals("accessibility", fields["renderMode"])
        assertEquals("good", fields["surfaceReadability"])
        assertEquals(true, fields["visualAvailable"])
    }

    @Test
    fun clickFieldsIncludeCompactPostActionState() {
        val fields = GhosthandApiPayloads.clickFields(
            ClickAttemptResult.success(
                attemptedPath = "node_click",
                effect = ActionEffectObservation(
                    stateChanged = true,
                    beforeSnapshotToken = "snap-before",
                    afterSnapshotToken = "snap-after",
                    finalPackageName = "com.example.target",
                    finalActivity = "TargetActivity"
                )
            )
        )

        val postActionState = fields["postActionState"] as Map<*, *>
        assertEquals("com.example.target", postActionState["packageName"])
        assertEquals("TargetActivity", postActionState["activity"])
        assertEquals("snap-after", postActionState["snapshotToken"])
    }

    @Test
    fun inputResultFieldsIncludeCompactPostActionState() {
        val fields = GhosthandApiPayloads.inputResultFields(
            InputOperationResult(
                performed = true,
                textMutation = InputTextMutationResult(
                    requested = true,
                    performed = true,
                    action = "set",
                    previousText = "old",
                    finalText = "new",
                    backendUsed = "accessibility",
                    failureReason = null,
                    attemptedPath = "focused_set_text"
                ),
                keyDispatch = null,
                postActionState = PostActionState(
                    packageName = "com.example.target",
                    activity = "EditorActivity",
                    snapshotToken = "snap-after"
                )
            )
        )

        val postActionState = fields["postActionState"] as Map<*, *>
        assertEquals("com.example.target", postActionState["packageName"])
        assertEquals("EditorActivity", postActionState["activity"])
        assertEquals("snap-after", postActionState["snapshotToken"])
    }

    @Test
    fun clickFailureFieldsExposeBoundedFailureEvidence() {
        val fields = GhosthandApiPayloads.clickFailureFields(
            FindMissHint(
                searchedSurface = "text",
                matchSemantics = "exact",
                failureCategory = "actionable_target_not_found",
                selectorMatchCount = 1,
                actionableMatchCount = 0
            )
        )

        assertEquals("actionable_target_not_found", fields["failureCategory"])
        assertEquals(1, fields["selectorMatchCount"])
        assertEquals(0, fields["actionableMatchCount"])
    }

    @Test
    fun disclosureJsonSerializesCompactDisclosureShape() {
        val disclosure = GhosthandDisclosure(
            kind = "discoverability",
            summary = "This route only searched the requested selector surface.",
            assumptionToCorrect = "Visible labels always live on the same selector surface.",
            nextBestActions = listOf("Retry with desc.", "Retry with text.")
        )

        val fields = GhosthandApiPayloads.disclosureFields(disclosure)

        assertEquals("discoverability", fields["kind"])
        assertEquals("This route only searched the requested selector surface.", fields["summary"])
        assertEquals(
            "Visible labels always live on the same selector surface.",
            fields["assumptionToCorrect"]
        )
        assertEquals(2, (fields["nextBestActions"] as List<*>).size)
    }

    @Test
    fun disclosureFieldsAllowOmittedOptionalAssumption() {
        val disclosure = GhosthandDisclosure(
            kind = "ambiguity",
            summary = "The gesture dispatched, but visible-state change was not observed."
        )

        val fields = GhosthandApiPayloads.disclosureFields(disclosure)

        assertEquals("ambiguity", fields["kind"])
        assertEquals("The gesture dispatched, but visible-state change was not observed.", fields["summary"])
        assertNull(fields["assumptionToCorrect"])
        assertTrue((fields["nextBestActions"] as List<*>).isEmpty())
    }

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
        assertEquals(false, payload["partialOutput"])
        assertEquals(1, payload["candidateNodeCount"])
        assertEquals(1, payload["returnedElementCount"])
        val button = elements.first() as Map<*, *>
        assertEquals("Button", button["text"])
        assertEquals(10, button["centerX"])
        assertEquals(20, button["centerY"])
        assertEquals("[0,0][100,100]", button["bounds"])
        assertEquals("accessibility", button["source"])
        assertEquals(true, payload["foregroundStableDuringCapture"])
    }

    @Test
    fun screenSummaryFieldsOmitElementsAndExposeCompactOrientationData() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Editor", editable = true, focused = true, centerX = 30, centerY = 40),
                node("p0.1@tsnap", text = "Submit", clickable = true, centerX = 50, centerY = 60)
            )
        )

        val fullPayload = GhosthandApiPayloads.accessibilityScreenRead(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )
        val summary = GhosthandApiPayloads.screenSummaryFields(fullPayload)

        assertEquals("com.example", summary["packageName"])
        assertEquals("ExampleActivity", summary["activity"])
        assertEquals("snap", summary["snapshotToken"])
        assertEquals(3, summary["candidateNodeCount"])
        assertEquals(2, summary["returnedElementCount"])
        assertEquals(true, summary["focusedEditablePresent"])
        assertEquals("accessibility", summary["renderMode"])
        assertEquals("limited", summary["surfaceReadability"])
        assertEquals(null, summary["visualAvailable"])
        assertFalse(summary.containsKey("elements"))
    }

    @Test
    fun screenSummaryFieldsExposePreviewMetadataWithoutImageBytes() {
        val summary = GhosthandApiPayloads.screenSummaryFields(
            GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot(
                    nodes = listOf(
                        node("p0@tsnap"),
                        node("p0.0@tsnap", text = "Preview", clickable = true, centerX = 10, centerY = 20)
                    )
                ),
                editableOnly = false,
                scrollableOnly = false,
                packageFilter = null,
                clickableOnly = false
            ).copy(
                visualAvailable = true,
                previewAvailable = true,
                previewToken = "preview:snap",
                previewWidth = 240,
                previewHeight = 240,
                previewImage = "data:image/png;base64,thumb"
            )
        )

        assertEquals(true, summary["previewAvailable"])
        assertEquals("preview:snap", summary["previewToken"])
        assertEquals(240, summary["previewWidth"])
        assertEquals(240, summary["previewHeight"])
        assertFalse(summary.containsKey("previewImage"))
    }

    @Test
    fun screenSummaryFocusedEditablePresentRequiresFocusedEditable() {
        val summary = GhosthandApiPayloads.screenSummaryFields(
            GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot(
                    nodes = listOf(
                        node("p0@tsnap"),
                        node("p0.0@tsnap", text = "Editor", editable = true, focused = false, centerX = 10, centerY = 20)
                    )
                ),
                editableOnly = false,
                scrollableOnly = false,
                packageFilter = null,
                clickableOnly = false
            )
        )

        assertEquals(false, summary["focusedEditablePresent"])
    }

    @Test
    fun renderModeAndSurfaceReadabilityExposeCanonicalWireValues() {
        val payload = GhosthandApiPayloads.accessibilityScreenRead(
            snapshot = snapshot(
                nodes = listOf(
                    node("p0@tsnap"),
                    node("p0.0@tsnap", text = "Only visible node", centerX = 10, centerY = 20)
                )
            ),
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        ).copy(
            partialOutput = true,
            candidateNodeCount = 30,
            returnedElementCount = 1,
            omittedNodeCount = 20,
            omittedLowSignalCount = 20,
            warnings = listOf("partial_output"),
            retryHint = ScreenReadRetryHint(
                source = ScreenReadMode.HYBRID.wireValue,
                reason = "accessibility_operationally_insufficient"
            )
        )

        assertEquals(GhosthandRenderMode.LIMITED_ACCESSIBILITY, payload.renderModeKind())
        assertEquals(GhosthandRenderMode.LIMITED_ACCESSIBILITY.wireValue, payload.renderMode())
        assertEquals(GhosthandSurfaceReadability.LIMITED, payload.surfaceReadabilityKind())
        assertEquals(GhosthandSurfaceReadability.LIMITED.wireValue, payload.surfaceReadability())
    }

    @Test
    fun screenPayloadIncludesRenderModeReadabilityAndVisualAvailability() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Visible", clickable = true, centerX = 10, centerY = 20)
            )
        )

        val payload = GhosthandApiPayloads.screenReadFields(
            GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot,
                editableOnly = false,
                scrollableOnly = false,
                packageFilter = null,
                clickableOnly = false
            ).copy(visualAvailable = true)
        )

        assertEquals("limited_accessibility", payload["renderMode"])
        assertEquals("limited", payload["surfaceReadability"])
        assertEquals(true, payload["visualAvailable"])
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
    fun screenPayloadOmitsNodesWithInvalidActionBoundsAndSignalsWarning() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Good", clickable = true, centerX = 10, centerY = 20),
                node(
                    "p0.1@tsnap",
                    text = "Bad",
                    clickable = true,
                    centerX = -5,
                    centerY = 20,
                    bounds = NodeBounds(-10, 0, 5, 40)
                )
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        val warnings = payload["warnings"] as List<*>
        val elements = payload["elements"] as List<*>
        assertTrue(warnings.contains("omitted_nodes_with_invalid_bounds"))
        assertTrue(warnings.contains("partial_output"))
        assertEquals(true, payload["foregroundStableDuringCapture"])
        assertEquals(true, payload["partialOutput"])
        assertEquals(listOf("invalid_bounds", "low_signal"), payload["omittedCategories"])
        assertEquals("Omitted 1 invalid-bounds and 1 low-signal nodes.", payload["omittedSummary"])
        assertEquals(true, payload["invalidBoundsPresent"])
        assertEquals(true, payload["lowSignalPresent"])
        assertEquals(3, payload["candidateNodeCount"])
        assertEquals(1, payload["returnedElementCount"])
        assertEquals(1, payload["omittedInvalidBoundsCount"])
        assertEquals(2, payload["omittedNodeCount"])
        assertEquals(1, elements.size)
        val kept = elements.first() as Map<*, *>
        assertEquals("Good", kept["text"])
    }

    @Test
    fun treePayloadFlagsInvalidBoundsWithoutDroppingNodes() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", bounds = NodeBounds(0, 0, 100, 100)),
                node("p0.0@tsnap", text = "Broken", bounds = NodeBounds(100, 50, 100, 80))
            )
        )

        val payload = GhosthandApiPayloads.treeFields(snapshot)
        val warnings = payload["warnings"] as List<*>
        val nodes = payload["nodes"] as List<*>
        assertTrue(warnings.contains("invalid_bounds_present"))
        assertTrue(warnings.contains("low_signal_nodes_present"))
        assertEquals(true, payload["foregroundStableDuringCapture"])
        assertEquals(false, payload["partialOutput"])
        assertEquals(2, payload["returnedNodeCount"])
        assertEquals(1, payload["invalidBoundsCount"])
        assertEquals(1, payload["lowSignalCount"])
        assertEquals(2, nodes.size)
        val broken = nodes[1] as Map<*, *>
        assertFalse(broken["boundsValid"] as Boolean)
        assertFalse(broken["actionableBounds"] as Boolean)
    }

    @Test
    fun screenPayloadOmitsLowSignalNodesAndSignalsWarning() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", text = "Root"),
                node("p0.0@tsnap", text = "Visible label", clickable = true, centerX = 10, centerY = 20),
                node("p0.1@tsnap", clickable = false, centerX = 30, centerY = 40)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        val warnings = payload["warnings"] as List<*>
        val elements = payload["elements"] as List<*>
        assertTrue(warnings.contains("omitted_low_signal_nodes"))
        assertTrue(warnings.contains("partial_output"))
        assertEquals(true, payload["partialOutput"])
        assertEquals(listOf("low_signal"), payload["omittedCategories"])
        assertEquals("Omitted 1 low-signal nodes.", payload["omittedSummary"])
        assertEquals(false, payload["invalidBoundsPresent"])
        assertEquals(true, payload["lowSignalPresent"])
        assertEquals(3, payload["candidateNodeCount"])
        assertEquals(2, payload["returnedElementCount"])
        assertEquals(1, payload["omittedLowSignalCount"])
        assertEquals(2, elements.size)
        assertTrue(
            elements.any { candidate ->
                (candidate as Map<*, *>)["text"] == "Visible label"
            }
        )
    }

    @Test
    fun screenPayloadKeepsClickableUnlabeledNodesAsActionRelevant() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", clickable = true, centerX = 30, centerY = 40)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = true
        )

        val warnings = payload["warnings"] as List<*>
        val elements = payload["elements"] as List<*>
        assertFalse(warnings.contains("omitted_low_signal_nodes"))
        assertEquals(false, payload["partialOutput"])
        assertEquals(emptyList<String>(), payload["omittedCategories"])
        assertEquals(null, payload["omittedSummary"])
        assertEquals(false, payload["invalidBoundsPresent"])
        assertEquals(false, payload["lowSignalPresent"])
        assertEquals(1, payload["candidateNodeCount"])
        assertEquals(1, payload["returnedElementCount"])
        assertEquals(0, payload["omittedLowSignalCount"])
        assertEquals(1, elements.size)
        val kept = elements.first() as Map<*, *>
        assertEquals("p0.0@tsnap", kept["nodeId"])
        assertEquals("", kept["text"])
    }

    @Test
    fun screenPayloadOmitsRetryHintForOrdinaryPartialOutput() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", text = "Root"),
                node("p0.0@tsnap", text = "Visible label", clickable = true, centerX = 10, centerY = 20),
                node("p0.1@tsnap", clickable = false, centerX = 30, centerY = 40)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        assertEquals(true, payload["partialOutput"])
        assertEquals(2, payload["returnedElementCount"])
        assertNull(payload["suggestedFallback"])
        assertNull(payload["suggestedSource"])
        assertNull(payload["fallbackReason"])
        assertNull(payload["retryHint"])
    }

    @Test
    fun screenPayloadAddsHybridRetryHintWhenAccessibilityIsOperationallyInsufficient() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", text = "Only visible node", clickable = true, centerX = 10, centerY = 20),
                node("p0.1@tsnap", clickable = false, centerX = 30, centerY = 40),
                node("p0.2@tsnap", clickable = false, centerX = 50, centerY = 60)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        val retryHint = payload["retryHint"] as Map<*, *>
        assertEquals(true, payload["partialOutput"])
        assertEquals(1, payload["returnedElementCount"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["suggestedFallback"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["suggestedSource"])
        assertEquals("accessibility_operationally_insufficient", payload["fallbackReason"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, retryHint["source"])
        assertEquals("accessibility_operationally_insufficient", retryHint["reason"])
    }

    @Test
    fun screenPayloadAddsOcrRetryHintWhenAccessibilityOutputIsEmpty() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap"),
                node("p0.0@tsnap", clickable = false, centerX = 30, centerY = 40)
            )
        )

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        val retryHint = payload["retryHint"] as Map<*, *>
        assertEquals(true, payload["partialOutput"])
        assertEquals(0, payload["returnedElementCount"])
        assertEquals(ScreenReadMode.OCR.wireValue, payload["suggestedFallback"])
        assertEquals(ScreenReadMode.OCR.wireValue, payload["suggestedSource"])
        assertEquals("accessibility_empty", payload["fallbackReason"])
        assertEquals(ScreenReadMode.OCR.wireValue, retryHint["source"])
        assertEquals("accessibility_empty", retryHint["reason"])
    }

    @Test
    fun screenPayloadAddsHybridRetryHintWhenLargePortionOfAccessibilityOutputIsOmitted() {
        val visibleNodes = (0 until 70).map { index ->
            node(
                nodeId = "p0.$index@tsnap",
                text = "Visible $index",
                clickable = true,
                centerX = 10 + index,
                centerY = 20 + index
            )
        }
        val lowSignalNodes = (70 until 133).map { index ->
            node(
                nodeId = "p0.$index@tsnap",
                clickable = false,
                centerX = 10 + index,
                centerY = 20 + index
            )
        }
        val snapshot = snapshot(nodes = listOf(node("p0@tsnap")) + visibleNodes + lowSignalNodes)

        val payload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )

        val retryHint = payload["retryHint"] as Map<*, *>
        assertEquals(true, payload["partialOutput"])
        assertEquals(134, payload["candidateNodeCount"])
        assertEquals(70, payload["returnedElementCount"])
        assertEquals(64, payload["omittedNodeCount"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["suggestedFallback"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, payload["suggestedSource"])
        assertEquals("accessibility_operationally_insufficient", payload["fallbackReason"])
        assertEquals(ScreenReadMode.HYBRID.wireValue, retryHint["source"])
        assertEquals("accessibility_operationally_insufficient", retryHint["reason"])
    }

    @Test
    fun treePayloadFlagsLowSignalNodesWithoutPretendingTheyAreUseful() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", text = "Root"),
                node("p0.0@tsnap"),
                node("p0.1@tsnap", text = "Signal")
            )
        )

        val payload = GhosthandApiPayloads.treeFields(snapshot)
        val warnings = payload["warnings"] as List<*>
        val nodes = payload["nodes"] as List<*>
        assertTrue(warnings.contains("low_signal_nodes_present"))
        assertEquals(false, payload["partialOutput"])
        assertEquals(3, payload["returnedNodeCount"])
        assertEquals(1, payload["lowSignalCount"])
        val lowSignalNode = nodes[1] as Map<*, *>
        assertEquals(true, lowSignalNode["lowSignal"])
        val signalNode = nodes[2] as Map<*, *>
        assertEquals(false, signalNode["lowSignal"])
    }

    @Test
    fun treePayloadDoesNotMarkClickableUnlabeledNodesAsLowSignal() {
        val snapshot = snapshot(
            nodes = listOf(
                node("p0@tsnap", text = "Root"),
                node("p0.0@tsnap", clickable = true)
            )
        )

        val payload = GhosthandApiPayloads.treeFields(snapshot)
        val warnings = payload["warnings"] as List<*>
        val nodes = payload["nodes"] as List<*>
        assertFalse(warnings.contains("low_signal_nodes_present"))
        assertEquals(false, payload["partialOutput"])
        assertEquals(2, payload["returnedNodeCount"])
        assertEquals(0, payload["lowSignalCount"])
        val clickableNode = nodes[1] as Map<*, *>
        assertEquals(false, clickableNode["lowSignal"])
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
    fun findPayloadForNonZeroIndexKeepsSelectedMatchGeometry() {
        val secondMatch = node(
            nodeId = "p0.1@tsnap",
            text = "Row",
            centerX = 25,
            centerY = 35,
            bounds = NodeBounds(10, 20, 40, 50)
        )
        val payload = GhosthandApiPayloads.findFields(
            FindNodeResult(
                found = true,
                node = secondMatch,
                matches = listOf(
                    node(
                        nodeId = "p0.0@tsnap",
                        text = "Row",
                        centerX = 0,
                        centerY = 0,
                        bounds = NodeBounds(0, 0, 0, 0)
                    ),
                    secondMatch
                ),
                selectedIndex = 1
            )
        )

        assertEquals(1, payload["index"])
        assertEquals("[10,20][40,50]", payload["bounds"])
        assertEquals(25, payload["centerX"])
        assertEquals(35, payload["centerY"])
    }

    @Test
    fun findPayloadUsesNullNodeWhenNoMatchExists() {
        val payload = GhosthandApiPayloads.findFields(
            FindNodeResult(
                found = false,
                node = null,
                matches = emptyList(),
                selectedIndex = 0,
                missHint = FindMissHint(
                    searchedSurface = "text",
                    matchSemantics = "exact",
                    requestedSurface = "text",
                    requestedMatchSemantics = "exact",
                    matchedSurface = "contentDesc",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    suggestedAlternateStrategies = listOf("textContains")
                )
            )
        )
        assertEquals(null, payload["node"])
        assertEquals(0, payload["matchCount"])
        assertEquals("text", payload["searchedSurface"])
        assertEquals("exact", payload["matchSemantics"])
        assertEquals("text", payload["requestedSurface"])
        assertEquals("exact", payload["requestedMatchSemantics"])
        assertEquals("contentDesc", payload["matchedSurface"])
        assertEquals("contains", payload["matchedMatchSemantics"])
        assertEquals(true, payload["usedSurfaceFallback"])
        assertEquals(true, payload["usedContainsFallback"])
        assertEquals(listOf("textContains"), payload["suggestedAlternateStrategies"])
    }

    @Test
    fun clickResolutionFieldsExposeInspectableSelectorMetadata() {
        val resolution = GhosthandApiPayloads.clickResolutionFields(
            ClickSelectorResolution(
                requestedStrategy = "text",
                effectiveStrategy = "textContains",
                requestedSurface = "text",
                matchedSurface = "text",
                requestedMatchSemantics = "exact",
                matchedMatchSemantics = "contains",
                usedSurfaceFallback = false,
                usedContainsFallback = true,
                matchedNodeId = "p0.0.0@tsnap",
                matchedNodeClickable = false,
                resolvedNodeId = "p0.0@tsnap",
                resolutionKind = "clickable_ancestor",
                ancestorDepth = 1
            )
        )

        assertEquals("text", resolution["requestedStrategy"])
        assertEquals("textContains", resolution["effectiveStrategy"])
        assertEquals("text", resolution["requestedSurface"])
        assertEquals("text", resolution["matchedSurface"])
        assertEquals("exact", resolution["requestedMatchSemantics"])
        assertEquals("contains", resolution["matchedMatchSemantics"])
        assertEquals(false, resolution["usedSurfaceFallback"])
        assertEquals(true, resolution["usedContainsFallback"])
        assertEquals("p0.0.0@tsnap", resolution["matchedNodeId"])
        assertEquals(false, resolution["matchedNodeClickable"])
        assertEquals("p0.0@tsnap", resolution["resolvedNodeId"])
        assertEquals("clickable_ancestor", resolution["resolutionKind"])
        assertEquals(1, resolution["ancestorDepth"])
    }

    @Test
    fun findPayloadIncludesMatchedSurfaceTruthForSuccessfulFallbackSearch() {
        val match = node(
            nodeId = "p0.1@tsnap",
            text = "Settings",
            clickable = true
        )
        val payload = GhosthandApiPayloads.findFields(
            FindNodeResult(
                found = true,
                node = match,
                matches = listOf(match),
                selectedIndex = 0,
                missHint = FindMissHint(
                    searchedSurface = "contentDesc",
                    matchSemantics = "exact",
                    requestedSurface = "contentDesc",
                    requestedMatchSemantics = "exact",
                    matchedSurface = "text",
                    matchedMatchSemantics = "exact",
                    usedSurfaceFallback = true,
                    usedContainsFallback = false
                )
            )
        )

        assertEquals(true, payload["found"])
        assertEquals("contentDesc", payload["requestedSurface"])
        assertEquals("text", payload["matchedSurface"])
        assertEquals("exact", payload["matchedMatchSemantics"])
        assertEquals(true, payload["usedSurfaceFallback"])
        assertEquals(false, payload["usedContainsFallback"])
        assertEquals("Settings", payload["text"])
    }

    @Test
    fun screenAndTreePayloadsIncludeFreshnessWarningsFromSnapshot() {
        val snapshot = AccessibilityTreeSnapshot(
            packageName = "com.example",
            activity = null,
            snapshotToken = "snap",
            capturedAt = "2026-03-28T00:00:00Z",
            nodes = listOf(node("p0@tsnap")),
            foregroundStableDuringCapture = false,
            freshnessWarnings = listOf("foreground_changed_during_capture")
        )

        val screenPayload = GhosthandApiPayloads.screenFields(
            snapshot = snapshot,
            editableOnly = false,
            scrollableOnly = false,
            packageFilter = null,
            clickableOnly = false
        )
        val treePayload = GhosthandApiPayloads.treeFields(snapshot)

        val screenWarnings = screenPayload["warnings"] as List<*>
        val treeWarnings = treePayload["warnings"] as List<*>
        assertEquals(false, screenPayload["foregroundStableDuringCapture"])
        assertTrue(screenWarnings.contains("foreground_changed_during_capture"))
        assertTrue(screenWarnings.contains("omitted_low_signal_nodes"))
        assertEquals(true, screenPayload["partialOutput"])
        assertEquals(false, treePayload["foregroundStableDuringCapture"])
        assertTrue(treeWarnings.contains("foreground_changed_during_capture"))
        assertTrue(treeWarnings.contains("low_signal_nodes_present"))
        assertEquals(false, treePayload["partialOutput"])
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
        focused: Boolean = false,
        scrollable: Boolean = false,
        centerX: Int = 0,
        centerY: Int = 0,
        bounds: NodeBounds = NodeBounds(0, 0, 100, 100)
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
            focused = focused,
            scrollable = scrollable,
            centerX = centerX,
            centerY = centerY,
            bounds = bounds
        )
    }
}
