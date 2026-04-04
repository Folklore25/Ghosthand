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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardReadFallbackStateTest {
    @Test
    fun normalClipboardReadReturnsSystemTextAndClearsFallback() {
        val state = ClipboardReadFallbackState()
        state.recordSuccessfulWrite("ghosthand clip path")

        val liveRead = state.resolveRead(itemCount = 1, text = "system text")
        assertTrue(liveRead.available)
        assertEquals("system text", liveRead.text)
        assertEquals("clipboard_read", liveRead.attemptedPath)

        val nextEmptyRead = state.resolveRead(itemCount = 0, text = null)
        assertFalse(nextEmptyRead.available)
        assertNull(nextEmptyRead.text)
        assertEquals("clipboard_empty", nextEmptyRead.attemptedPath)
    }

    @Test
    fun emptyClipboardFallsBackOnceAfterSuccessfulWrite() {
        val state = ClipboardReadFallbackState()
        state.recordSuccessfulWrite("ghosthand clip path")

        val fallbackRead = state.resolveRead(itemCount = 0, text = null)
        assertTrue(fallbackRead.available)
        assertEquals("ghosthand clip path", fallbackRead.text)
        assertEquals("clipboard_cached_after_write", fallbackRead.attemptedPath)

        val secondEmptyRead = state.resolveRead(itemCount = 0, text = null)
        assertFalse(secondEmptyRead.available)
        assertNull(secondEmptyRead.text)
        assertEquals("clipboard_empty", secondEmptyRead.attemptedPath)
    }

    @Test
    fun emptyClipboardWithoutPriorWriteDoesNotInventText() {
        val state = ClipboardReadFallbackState()

        val result = state.resolveRead(itemCount = 0, text = null)
        assertFalse(result.available)
        assertNull(result.text)
        assertEquals("clipboard_empty", result.attemptedPath)
    }
}
