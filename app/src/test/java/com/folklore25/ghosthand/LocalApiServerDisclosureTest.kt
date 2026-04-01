/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalApiServerDisclosureTest {
    @Test
    fun waitUiChangeDisclosureClarifiesChangedFalse() {
        val disclosure = buildWaitUiChangeDisclosure(
            StateCoordinator.WaitUiChangeResult(
                changed = false,
                outcome = WaitOutcome.forUiChange(
                    stateChanged = false,
                    timedOut = true
                ),
                elapsedMs = 1200,
                snapshotToken = "snap",
                packageName = "pkg",
                activity = "Activity"
            )
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertEquals("`changed=false` means the action failed.", disclosure.assumptionToCorrect)
        assertEquals(2, disclosure.nextBestActions.size)
    }

    @Test
    fun waitConditionDisclosureClarifiesSelectorWaitSemantics() {
        val disclosure = buildWaitConditionDisclosure(
            strategy = "text",
            result = NormalizedWaitConditionResult(
                satisfied = false,
                conditionMet = false,
                stateChanged = false,
                timedOut = true,
                node = null,
                reason = "timeout"
            )
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("POST /wait"))
        assertTrue(disclosure.nextBestActions.first().contains("GET /wait"))
    }

    @Test
    fun normalizeWaitConditionResultRejectsConditionMetTrueWithoutNode() {
        val normalized = normalizeWaitConditionResult(
            StateCoordinator.WaitConditionResult(
                satisfied = true,
                outcome = WaitOutcome.forCondition(
                    conditionMet = true,
                    initialState = UiStateSnapshot("snap1", "pkg", "Activity"),
                    finalState = UiStateSnapshot("snap1", "pkg", "Activity"),
                    timedOut = false
                ),
                node = null,
                elapsedMs = 100,
                polledCount = 1,
                attemptedPath = "condition_met"
            )
        )

        assertEquals(false, normalized.satisfied)
        assertEquals(false, normalized.conditionMet)
        assertEquals(true, normalized.timedOut.not())
        assertEquals(null, normalized.node)
        assertEquals("condition_met", normalized.reason)
    }

    @Test
    fun findDisclosureExplainsClickableOnlyResolution() {
        val disclosure = buildFindDisclosure(
            strategy = "text",
            clickableOnly = true,
            result = FindNodeResult(
                found = false,
                node = null,
                missHint = FindMissHint(
                    searchedSurface = "text",
                    matchSemantics = "exact"
                )
            )
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("clickable=true"))
    }

    @Test
    fun findDisclosureExplainsExactTextMissOnLongerTextBlock() {
        val disclosure = buildFindDisclosure(
            strategy = "text",
            clickableOnly = false,
            result = FindNodeResult(
                found = false,
                node = null,
                missHint = FindMissHint(
                    searchedSurface = "text",
                    matchSemantics = "exact",
                    likelyMissReason = "visible_text_is_part_of_a_longer_text_block",
                    suggestedAlternateStrategies = listOf("textContains")
                )
            )
        )

        assertNotNull(disclosure)
        assertTrue(disclosure!!.summary.contains("exact text"))
        assertEquals("/screen-visible text always matches exact /find text.", disclosure.assumptionToCorrect)
        assertTrue(disclosure.nextBestActions.first().contains("textContains"))
    }

    @Test
    fun clickDisclosureExplainsWrapperFallback() {
        val disclosure = buildClickDisclosure(
            strategy = "text",
            clickableOnly = true,
            result = ClickAttemptResult.success(
                attemptedPath = "node_click",
                selectorResolution = ClickSelectorResolution(
                    requestedStrategy = "text",
                    effectiveStrategy = "textContains",
                    usedContainsFallback = true,
                    matchedNodeId = "p0.0.0@tsnap",
                    matchedNodeClickable = false,
                    resolvedNodeId = "p0.0@tsnap",
                    resolutionKind = "clickable_ancestor",
                    ancestorDepth = 1
                )
            )
        )

        assertNotNull(disclosure)
        assertEquals("fallback", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("clickable ancestor"))
    }

    @Test
    fun clickDisclosureExplainsCrossSurfaceFallback() {
        val disclosure = buildClickDisclosure(
            strategy = "text",
            clickableOnly = true,
            result = ClickAttemptResult.success(
                attemptedPath = "node_click",
                selectorResolution = ClickSelectorResolution(
                    requestedStrategy = "text",
                    effectiveStrategy = "contentDescContains",
                    requestedSurface = "text",
                    matchedSurface = "contentDesc",
                    requestedMatchSemantics = "exact",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    matchedNodeId = "p0.0.0@tsnap",
                    matchedNodeClickable = false,
                    resolvedNodeId = "p0.0@tsnap",
                    resolutionKind = "clickable_ancestor",
                    ancestorDepth = 1
                )
            )
        )

        assertNotNull(disclosure)
        assertEquals("fallback", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("another surface"))
        assertEquals(
            "The meaningful label must live on the exact selector surface I requested.",
            disclosure.assumptionToCorrect
        )
    }

    @Test
    fun screenDisclosureExplainsReducedOutput() {
        val disclosure = buildScreenDisclosure(
            partialOutput = true,
            foregroundStableDuringCapture = true
        )

        assertNotNull(disclosure)
        assertEquals("shaped_output", disclosure!!.kind)
        assertTrue(disclosure.nextBestActions.first().contains("/tree"))
    }

    @Test
    fun motionDisclosureClarifiesPerformedWithoutObservedChange() {
        val disclosure = buildMotionDisclosure(
            route = "/scroll",
            performed = true,
            surfaceChanged = false
        )

        assertNotNull(disclosure)
        assertEquals("ambiguity", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("/scroll"))
        assertEquals("`performed=true` proves the content advanced.", disclosure.assumptionToCorrect)
    }

    @Test
    fun motionDisclosureIsOmittedWhenSurfaceChanged() {
        assertNull(
            buildMotionDisclosure(
                route = "/swipe",
                performed = true,
                surfaceChanged = true
            )
        )
    }
}
