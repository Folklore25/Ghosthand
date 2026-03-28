/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityNodeLocatorTest {
    @Test
    fun pathSegmentsParsesSnapshotScopedNodeIds() {
        val segments = AccessibilityNodeLocator.pathSegments("p0.4.2@tabcd1234")
        assertEquals(listOf(0, 4, 2), segments)
    }

    @Test
    fun pathSegmentsReturnsEmptyForLegacyNodeIds() {
        val segments = AccessibilityNodeLocator.pathSegments("n42")
        assertTrue(segments.isEmpty())
    }

    @Test
    fun createNodeIdEncodesPathAndToken() {
        val nodeId = AccessibilityNodeLocator.createNodeId(listOf(0, 1, 3), "token123")
        assertEquals("p0.1.3@ttoken123", nodeId)
    }
}
