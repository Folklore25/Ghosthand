/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GhosthandSelectorsTest {
    @Test
    fun normalizePrefersTextAliasFirst() {
        val selector = GhosthandSelectors.normalize(
            text = "Send",
            desc = "ignored",
            id = "ignored",
            strategy = "contentDesc",
            query = "ignored"
        )

        assertEquals(SelectorQuery("text", "Send"), selector)
    }

    @Test
    fun normalizeMapsDescAndIdAliasesToInternalStrategies() {
        assertEquals(
            SelectorQuery("contentDesc", "Search"),
            GhosthandSelectors.normalize(
                text = null,
                desc = "Search",
                id = null,
                strategy = null,
                query = null
            )
        )
        assertEquals(
            SelectorQuery("resourceId", "android:id/input"),
            GhosthandSelectors.normalize(
                text = null,
                desc = null,
                id = "android:id/input",
                strategy = null,
                query = null
            )
        )
    }

    @Test
    fun normalizeFallsBackToExplicitStrategyAndQuery() {
        val selector = GhosthandSelectors.normalize(
            text = null,
            desc = null,
            id = null,
            strategy = "textContains",
            query = "wifi"
        )

        assertEquals(SelectorQuery("textContains", "wifi"), selector)
    }

    @Test
    fun normalizeReturnsNullWhenNoSelectorProvided() {
        assertNull(
            GhosthandSelectors.normalize(
                text = null,
                desc = null,
                id = null,
                strategy = null,
                query = null
            )
        )
    }
}
