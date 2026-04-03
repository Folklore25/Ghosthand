/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.state.StateCoordinator
import com.folklore25.ghosthand.wait.*
import com.folklore25.ghosthand.routes.action.buildActionEffectDisclosure
import com.folklore25.ghosthand.routes.action.buildClickDisclosure
import com.folklore25.ghosthand.routes.action.clickFailureErrorCode
import com.folklore25.ghosthand.routes.action.clickFailureMessage
import com.folklore25.ghosthand.routes.action.buildMotionDisclosure
import com.folklore25.ghosthand.routes.read.buildFindDisclosure
import com.folklore25.ghosthand.routes.read.buildScreenDisclosure
import com.folklore25.ghosthand.routes.wait.NormalizedWaitConditionResult
import com.folklore25.ghosthand.routes.wait.buildWaitConditionDisclosure
import com.folklore25.ghosthand.routes.wait.buildWaitUiChangeDisclosure
import com.folklore25.ghosthand.routes.wait.normalizeWaitConditionResult
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDisclosureBuildersTest {
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
        assertEquals(true, normalized.timedOut)
        assertEquals(null, normalized.node)
        assertEquals("timeout", normalized.reason)
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

    @Test
    fun actionEffectDisclosureClarifiesDispatchWithoutObservedStateChange() {
        val disclosure = buildActionEffectDisclosure(
            route = "/click",
            performed = true,
            stateChanged = false
        )

        assertNotNull(disclosure)
        assertEquals("ambiguity", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("/click"))
        assertEquals("`performed=true` proves the UI changed.", disclosure.assumptionToCorrect)
    }

    @Test
    fun postActionStatePrefersObservedSnapshotWhenAvailable() {
        val state = PostActionStateComposer.fromObservedEffect(
            actionEffect = ActionEffectObservation(
                stateChanged = true,
                beforeSnapshotToken = "before",
                afterSnapshotToken = "after",
                finalPackageName = "com.example.target",
                finalActivity = "TargetActivity"
            ),
            fallbackSnapshot = AccessibilityTreeSnapshot(
                packageName = "com.example.fallback",
                activity = "FallbackActivity",
                snapshotToken = "fallback",
                capturedAt = "2026-04-02T00:00:00Z",
                nodes = emptyList(),
                foregroundStableDuringCapture = true
            )
        )

        assertEquals("com.example.target", state?.packageName)
        assertEquals("TargetActivity", state?.activity)
        assertEquals("after", state?.snapshotToken)
    }

    @Test
    fun postActionStateFallsBackToCurrentSnapshotSubset() {
        val state = PostActionStateComposer.fromObservedEffect(
            actionEffect = null,
            fallbackSnapshot = AccessibilityTreeSnapshot(
                packageName = "com.example.target",
                activity = "TargetActivity",
                snapshotToken = "snap-after",
                capturedAt = "2026-04-02T00:00:00Z",
                nodes = emptyList(),
                foregroundStableDuringCapture = true
            )
        )

        assertEquals("com.example.target", state?.packageName)
        assertEquals("TargetActivity", state?.activity)
        assertEquals("snap-after", state?.snapshotToken)
    }

    @Test
    fun clickDisclosureExplainsActionabilityFailure() {
        val disclosure = buildClickDisclosure(
            strategy = "text",
            clickableOnly = true,
            result = ClickAttemptResult.failure(
                reason = ClickFailureReason.NODE_NOT_FOUND,
                attemptedPath = "selector_lookup",
                selectorMissHint = FindMissHint(
                    searchedSurface = "text",
                    matchSemantics = "exact",
                    failureCategory = "actionable_target_not_found",
                    selectorMatchCount = 1,
                    actionableMatchCount = 0
                )
            )
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("found label matches"))
        assertEquals(
            "A visible label match is always directly actionable.",
            disclosure.assumptionToCorrect
        )
    }

    @Test
    fun clickFailureCodeUsesStaleNodeReferenceForExpiredNodeId() {
        val result = ClickAttemptResult.failure(
            reason = ClickFailureReason.NODE_NOT_FOUND,
            attemptedPath = "stale_snapshot"
        )

        assertEquals(
            "STALE_NODE_REFERENCE",
            clickFailureErrorCode(
                result = result,
                nodeIdProvided = true
            )
        )
        assertEquals(
            "Click target node reference expired because the UI snapshot changed.",
            clickFailureMessage(
                result = result,
                nodeIdProvided = true
            )
        )
    }

    @Test
    fun clickFailureCodeKeepsNodeNotFoundForOrdinaryNodeIdMiss() {
        val result = ClickAttemptResult.failure(
            reason = ClickFailureReason.NODE_NOT_FOUND,
            attemptedPath = "node_lookup"
        )

        assertEquals(
            "NODE_NOT_FOUND",
            clickFailureErrorCode(
                result = result,
                nodeIdProvided = true
            )
        )
        assertEquals(
            "Click target node was not found.",
            clickFailureMessage(
                result = result,
                nodeIdProvided = true
            )
        )
    }

    @Test
    fun clickFailureCodeKeepsNodeNotFoundForSelectorMiss() {
        val result = ClickAttemptResult.failure(
            reason = ClickFailureReason.NODE_NOT_FOUND,
            attemptedPath = "selector_lookup"
        )

        assertEquals(
            "NODE_NOT_FOUND",
            clickFailureErrorCode(
                result = result,
                nodeIdProvided = false
            )
        )
        assertEquals(
            "Click target node was not found.",
            clickFailureMessage(
                result = result,
                nodeIdProvided = false
            )
        )
    }

}
