/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
