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
