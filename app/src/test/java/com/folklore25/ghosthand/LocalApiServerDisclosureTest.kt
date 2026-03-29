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
            result = StateCoordinator.WaitConditionResult(
                satisfied = false,
                node = null,
                elapsedMs = 5000,
                polledCount = 0,
                attemptedPath = "timeout"
            )
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("POST /wait"))
        assertTrue(disclosure.nextBestActions.first().contains("GET /wait"))
    }

    @Test
    fun findDisclosureExplainsClickableOnlyResolution() {
        val disclosure = buildFindDisclosure(
            strategy = "text",
            clickableOnly = true,
            found = false
        )

        assertNotNull(disclosure)
        assertEquals("discoverability", disclosure!!.kind)
        assertTrue(disclosure.summary.contains("clickable=true"))
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
